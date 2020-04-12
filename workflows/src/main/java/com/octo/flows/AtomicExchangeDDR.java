package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.InterBankTransferContract;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.InterBankTransferState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.NonEmptySet;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

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
            // Initiator flow logic goes here.
            VaultService vaultService = getServiceHub().getVaultService();
            centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));

            QueryCriteria vaultCriteria = new QueryCriteria.VaultQueryCriteria()
                    .withSoftLockingCondition(new QueryCriteria.SoftLockingCondition(QueryCriteria.SoftLockingType.UNLOCKED_ONLY, Collections.emptyList()))
                    .withExactParticipants(Arrays.asList(getOurIdentity(), centralBank));

            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), centralBank.getOwningKey(), receiverBank.getOwningKey());

            List<StateAndRef<DDRObjectState>> toArchiveDDR = vaultService.queryBy(DDRObjectState.class, vaultCriteria).getStates();

            vaultService.softLockRelease(getRunId().getUuid(), NonEmptySet.copyOf(toArchiveDDR.stream()
                    .map(StateAndRef::getRef).collect(Collectors.toList())));

            InterBankTransferState transferState = new InterBankTransferState(senderRIB, receiverRIB, getOurIdentity(),
                    receiverBank, amount, executionDate, executionDate + senderRIB.substring(0, 5) + receiverRIB.substring(0, 5));

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addOutputState(transferState)
                    .addCommand(new InterBankTransferContract.InterBankTransferCommands.BankTransfer(), requiredSigners);

            long addedAmount = addInputDDRToTransaction(txBuilder, toArchiveDDR, amount.getQuantity());
            boolean isTxBalanced = addRestDDROutput(addedAmount, txBuilder, transferState);
            addOutputDDRToTransaction(txBuilder, transferState);

            txBuilder.verify(getServiceHub());

            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final FlowSession centralBankSession = initiateFlow(centralBank);
            final FlowSession receiverBankSession = initiateFlow(receiverBank);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Arrays.asList(centralBankSession, receiverBankSession),
                    CollectSignaturesFlow.Companion.tracker()));
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(centralBankSession, receiverBankSession), StatesToRecord.ALL_VISIBLE));
        }

        private long addInputDDRToTransaction(TransactionBuilder txBuilder, List<StateAndRef<DDRObjectState>> queriedDDRs, long amount){
            long summed = 0;
            for(StateAndRef<DDRObjectState> ddr : queriedDDRs){
                if ( summed >= amount) return summed;
                txBuilder.addInputState(ddr);
                summed += ddr.getState().getData().getAmount().getQuantity();
            }
            return summed;
        }

        private void addOutputDDRToTransaction(TransactionBuilder txBuilder, InterBankTransferState transferState){
            produceDDRObjects(transferState).forEach(ddr -> txBuilder.addOutputState(ddr));
        }

        private List<DDRObjectState> produceDDRObjects(InterBankTransferState transferState) {
            Amount<Currency> transferAmount = transferState.getAmount();
            long quantity = transferAmount.getQuantity(); // For exmaple 1000 token
            DDRObjectStateBuilder builder = new DDRObjectStateBuilder();
            builder.issuer(centralBank).issuerDate(new Date()).owner(transferState.getReceiverBank())
                    .currency(transferAmount.getToken());
            if (quantity >= 1000) {
                int numberOfTokens = (int) Math.ceil((double) quantity / 1000);
                return transferAmount.splitEvenly(numberOfTokens).stream().map(am -> builder.amount(am.getQuantity()).build()).collect(Collectors.toList());
            }
            return Collections.singletonList(builder.amount(quantity).build());
        }

        private boolean addRestDDROutput(long consumedAmount, TransactionBuilder txBuilder, InterBankTransferState transferState){
            long amountToTransfer = transferState.getAmount().getQuantity();
            if(consumedAmount == amountToTransfer) return true;
            else if (consumedAmount < amountToTransfer) return false;
            Date issuerDate = new Date();
            DDRObjectState restDDR = new DDRObjectStateBuilder().owner(transferState.getSenderBank()).issuerDate(issuerDate)
                    .issuer(centralBank).currency(transferState.getAmount().getToken()).amount(consumedAmount - amountToTransfer).build();
            txBuilder.addOutputState(restDDR);
            return true;
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
