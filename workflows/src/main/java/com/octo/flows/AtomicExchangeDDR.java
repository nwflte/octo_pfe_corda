package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.InterBankTransferContract;
import com.octo.services.RIBService;
import com.octo.states.*;
import com.octo.utils.Utils;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
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

public class AtomicExchangeDDR {

    @InitiatingFlow
    @StartableByRPC
    @StartableByService
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String senderRIB;
        private final String receiverRIB;
        private Party receiverBank;
        private final Amount<Currency> amount;
        private final Date executionDate;
        private String reference = "";
        private final ProgressTracker progressTracker = new ProgressTracker();
        private Party centralBank;

        public Initiator(String senderRIB, String receiverRIB, Amount<Currency> amount, Date executionDate) {
            this.senderRIB = senderRIB;
            this.receiverRIB = receiverRIB;
            this.amount = amount;
            this.executionDate = executionDate;
        }

        public Initiator(String senderRIB, String receiverRIB, Amount<Currency> amount, Date executionDate, String reference) {
            this.senderRIB = senderRIB;
            this.receiverRIB = receiverRIB;
            this.amount = amount;
            this.executionDate = executionDate;
            this.reference = reference;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            centralBank = Utils.getCentralBankParty(getServiceHub());

            reference = reference.isEmpty() ? Utils.generateReference("INTER") : reference;
            receiverBank = getServiceHub().cordaService(RIBService.class).getPartyFromRIB(receiverRIB);
            if(receiverBank == null) subFlow(new SyncIdentitiesFlow.Initiator());
            receiverBank = getServiceHub().cordaService(RIBService.class).getPartyFromRIB(receiverRIB);
            if(receiverBank == null) throw new FlowException("Can't find party for rib " + receiverRIB);

            InterBankTransferState state = new InterBankTransferState(senderRIB, receiverRIB, getOurIdentity(),
                    receiverBank, amount, executionDate, reference);

            TransactionBuilder txBuilder = exchangeTx(state);

            final FlowSession centralBankSession = initiateFlow(centralBank);
            final FlowSession receiverBankSession = initiateFlow(receiverBank);

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
            for(int i = 0; i < ddrs.size() ; i++){
                StateAndRef<DDRObjectState> ddr = ddrs.get(i);
                txBuilder.addInputState(ddr);
                totalAmountConsumed += ddr.getState().getData().getAmount().getQuantity();
                if(i != lastIndex)
                    txBuilder.addOutputState(changeOwner(ddr.getState().getData(), receiverBank));
            }
            long amountMissingSender = totalAmountConsumed - amount.getQuantity();
            long amountMissingReceiver = amount.getQuantity() - totalAmountConsumed + ddrs.get(lastIndex).getState().getData().getAmount().getQuantity();
            return addRestDDROutput(txBuilder, amountMissingSender, amountMissingReceiver);
        }

        private DDRObjectState changeOwner(DDRObjectState ddrObjectState, Party newOwner){
            return new DDRObjectStateBuilder(ddrObjectState).owner(newOwner).build();
        }

        private TransactionBuilder addRestDDROutput(TransactionBuilder txBuilder, long amountMissingSender, long amountMissingReceiver){
            DDRObjectStateBuilder builder = new DDRObjectStateBuilder().issuer(centralBank) .currency(amount.getToken()).issuerDate(new Date());
            if(amountMissingSender != 0) {
                DDRObjectState senderRestDDR = builder.owner(getOurIdentity()).amount(amountMissingSender).build();
                txBuilder.addOutputState(senderRestDDR);
            }
            if(amountMissingReceiver != 0){
                DDRObjectState receiverRestDDR = builder.owner(receiverBank).amount(amountMissingReceiver).build();
                return txBuilder.addOutputState(receiverRestDDR);
            }
            return txBuilder;
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
