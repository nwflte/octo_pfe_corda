package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DenyDDRPledge {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final String externalId;

        private final ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(String externalId) {
            this.externalId = externalId;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Initiator flow logic goes here.
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, null,
                    Collections.singletonList(externalId), Vault.StateStatus.UNCONSUMED);
            List<StateAndRef<DDRObligationState>> inputStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(DDRObligationState.class, queryCriteria).getStates();

            final StateAndRef<DDRObligationState> ddrObligationStateStateAndRef = inputStateAndRefs.get(0);
            final DDRObligationState ddrObligationState = inputStateAndRefs.get(0).getState().getData();

            final Party ownerBank = ddrObligationState.getOwner();

            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), ownerBank.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(ddrObligationStateStateAndRef.getState().getNotary())
                    .addInputState(ddrObligationStateStateAndRef)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.DenyDDRPledge(), requiredSigners);

            txBuilder.verify(getServiceHub());

            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final FlowSession ownerBankSession = initiateFlow(ownerBank);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Collections.singletonList(ownerBankSession),
                    CollectSignaturesFlow.Companion.tracker()));

            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(ownerBankSession), StatesToRecord.ALL_VISIBLE));
        }
    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(com.octo.flows.DenyDDRPledge.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Responder flow logic goes here.
            final SecureHash txId = subFlow(new DenyDDRPledge.Responder.CheckTransactionAndSignFlow(counterpartySession, SignTransactionFlow.Companion.tracker())).getId();
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
