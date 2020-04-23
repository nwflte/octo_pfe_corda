package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.InterBankTransferContract;
import com.octo.states.*;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicExchangeDDR {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String senderRIB;
        private final String receiverRIB;
        private final Party receiverBank;
        private final Amount<Currency> amount;
        private final Date executionDate;
        private final ProgressTracker progressTracker = new ProgressTracker();
        private Party centralBank;

        public Initiator(String senderRIB, String receiverRIB, Party receiverBank, Amount<Currency> amount, Date executionDate) {
            this.senderRIB = senderRIB;
            this.receiverRIB = receiverRIB;
            this.receiverBank = receiverBank;
            this.amount = amount;
            this.executionDate = executionDate;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            centralBank = Utils.getCentralBankParty(getServiceHub());

            InterBankTransferState state = new InterBankTransferState(senderRIB, receiverRIB, getOurIdentity(),
                    receiverBank, amount, executionDate, Utils.generateReference("INTER"));

            TransactionBuilder txBuilder = exchangeTx(state);

            final FlowSession centralBankSession = initiateFlow(centralBank);
            final FlowSession receiverBankSession = initiateFlow(receiverBank);

            SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub(),
                    centralBankSession, receiverBankSession));

            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(centralBankSession, receiverBankSession), StatesToRecord.ALL_VISIBLE));
        }

        @Suspendable
        private TransactionBuilder exchangeTx(InterBankTransferState state) throws FlowException {

            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), centralBank.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addOutputState(state)
                    .addCommand(new InterBankTransferContract.InterBankTransferCommands.BankTransfer(), requiredSigners);

            AtomicLong totalAmountConsumed = new AtomicLong(0L);
            Utils.selectDDRs(getOurIdentity(), amount, getServiceHub(), txBuilder.getLockId()).forEach(ddr -> {
                txBuilder.addInputState(ddr);
                txBuilder.addOutputState(changeOwner(ddr.getState().getData(), receiverBank));
                totalAmountConsumed.addAndGet(ddr.getState().getData().getAmount().getQuantity());
            });
            return addRestDDROutput(txBuilder, totalAmountConsumed.get(), state);
        }

        private DDRObjectState changeOwner(DDRObjectState ddrObjectState, Party newOwner){
            return new DDRObjectStateBuilder(ddrObjectState).owner(newOwner).build();
        }

        private TransactionBuilder addRestDDROutput(TransactionBuilder txBuilder, long consumedAmount, InterBankTransferState transferState){
            long amountToTransfer = transferState.getAmount().getQuantity();
            if(consumedAmount == amountToTransfer) return txBuilder;
            DDRObjectState restDDR = new DDRObjectStateBuilder().owner(transferState.getSenderBank()).issuerDate(new Date())
                    .issuer(centralBank).currency(transferState.getAmount().getToken()).amount(consumedAmount - amountToTransfer).build();
            return txBuilder.addOutputState(restDDR);
        }
    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(com.octo.flows.AtomicExchangeDDR.Initiator.class)
    public static class CentralBankResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterpartySession;

        public CentralBankResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Responder flow logic goes here.
            final SecureHash txId = subFlow(new CheckTransactionAndSignFlow(counterpartySession, SignTransactionFlow.Companion.tracker())).getId();
            return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
        }

        private static class CheckTransactionAndSignFlow extends SignTransactionFlow {

            public CheckTransactionAndSignFlow(@NotNull FlowSession otherSideSession, @NotNull ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

            }
        }
    }


    // ******************
    // * Responder flow *
    // ******************
    //@InitiatedBy(com.octo.flows.AtomicExchangeDDR.Initiator.class)
    public static class BankResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterpartySession;

        public BankResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Responder flow logic goes here.
            final SecureHash txId = subFlow(new CheckTransactionAndSignFlow(counterpartySession, SignTransactionFlow.Companion.tracker())).getId();
            return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
        }

        private static class CheckTransactionAndSignFlow extends SignTransactionFlow {

            public CheckTransactionAndSignFlow(@NotNull FlowSession otherSideSession, @NotNull ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

            }
        }
    }

}
