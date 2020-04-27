package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.InterBankTransferContract;
import com.octo.services.BankComptesOracle;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.InterBankTransferState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.List;

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

            final FlowSession centralBankSession = initiateFlow(centralBank);
            final FlowSession receiverBankSession = initiateFlow(receiverBank);

            //Boolean receiverBankVerified = receiverBankSession.sendAndReceive(Boolean.class, state).unwrap(data -> data);
            //Boolean centralBankVerified = centralBankSession.sendAndReceive(Boolean.class, state).unwrap(data -> data);

            TransactionBuilder txBuilder = exchangeTx(state);

            SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub(),
                    centralBankSession, receiverBankSession));

            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(centralBankSession, receiverBankSession), StatesToRecord.ALL_VISIBLE));
        }

        @Suspendable
        private TransactionBuilder exchangeTx(InterBankTransferState state) throws FlowException {

            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), centralBank.getOwningKey(),
                    receiverBank.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addOutputState(state)
                    .addCommand(new InterBankTransferContract.InterBankTransferCommands.BankTransfer(), requiredSigners);

            long totalAmountConsumed = 0;
            List<StateAndRef<DDRObjectState>> ddrs = Utils.selectDDRs(getOurIdentity(), amount, getServiceHub(), txBuilder.getLockId());
            int lastIndex = ddrs.size() - 1;
            /*
            Add All DDRs but the last one to the receiver bank
             */
            for (int i = 0; i < ddrs.size(); i++) {
                StateAndRef<DDRObjectState> ddr = ddrs.get(i);
                txBuilder.addInputState(ddr);
                totalAmountConsumed += ddr.getState().getData().getAmount().getQuantity();
                if (i != lastIndex)
                    txBuilder.addOutputState(changeOwner(ddr.getState().getData(), receiverBank));
            }
            long amountMissingSender = totalAmountConsumed - amount.getQuantity();
            long amountMissingReceiver = amount.getQuantity() - totalAmountConsumed + ddrs.get(lastIndex).getState().getData().getAmount().getQuantity();
            return addRestDDROutput(txBuilder, amountMissingSender, amountMissingReceiver);
        }

        private DDRObjectState changeOwner(DDRObjectState ddrObjectState, Party newOwner) {
            return new DDRObjectStateBuilder(ddrObjectState).owner(newOwner).build();
        }

        private TransactionBuilder addRestDDROutput(TransactionBuilder txBuilder, long amountMissingSender, long amountMissingReceiver) {
            if (amountMissingSender == 0) return txBuilder;
            DDRObjectStateBuilder builder = new DDRObjectStateBuilder();
            DDRObjectState senderRestDDR = builder.owner(getOurIdentity()).issuerDate(new Date())
                    .issuer(centralBank).currency(amount.getToken()).amount(amountMissingSender).build();
            DDRObjectState receiverRestDDR = builder.owner(receiverBank).amount(amountMissingReceiver).build();
            return txBuilder.addOutputState(senderRestDDR).addOutputState(receiverRestDDR);
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
            /*InterBankTransferState transferData = counterpartySession.receive(InterBankTransferState.class)
                    .unwrap(data -> {
                        String ourOrgName = getOurIdentity().getName().getOrganisation();
                        if(!ourOrgName.equals("CentralBank") || !data.getReceiverBank().equals(getOurIdentity()))
                            throw new IllegalArgumentException("Transfer sent to wrong party : " + getOurIdentity());
                        return data;
                    });*/


            final SecureHash txId = subFlow(new CheckTransactionAndSignFlow(counterpartySession, SignTransactionFlow.Companion.tracker())).getId();
            return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
        }

        private static class CheckTransactionAndSignFlow extends SignTransactionFlow {

            public CheckTransactionAndSignFlow(@NotNull FlowSession otherSideSession, @NotNull ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                try {
                    LedgerTransaction ltx = stx.toLedgerTransaction(getServiceHub(), false);
                    InterBankTransferState state = ltx.outputsOfType(InterBankTransferState.class).get(0);
                    if(!checkTransferDataWithOracle(state))
                        throw new IllegalArgumentException("Party " + getOurIdentity() + " failed external verification");
                } catch (SignatureException | IOException e) {
                    throw new FlowException(e.getMessage());
                }
            }

            private boolean checkTransferDataWithOracle(InterBankTransferState transferState) throws IOException {
                return getServiceHub().cordaService(BankComptesOracle.class).
                        verifyAccountEligibleForTransfer(transferState, getOurIdentity());
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
