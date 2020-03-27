package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.octo.contracts.DDRObjectContract;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Date;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ApproveDDRPledge {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        // Idealy, we would pass the externalId as only parameter
        //private final Party requester;
        //private final Date requesterDate;
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
                    ImmutableList.of(externalId), Vault.StateStatus.UNCONSUMED);
            List<StateAndRef<DDRObligationState>> inputStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(DDRObligationState.class, queryCriteria).getStates();

            requireThat(require -> {
               require.using("Duplicate Obligation with same externalId", inputStateAndRefs.size() == 1);
                final DDRObligationState ddrObligationState = inputStateAndRefs.get(0).getState().getData();
               require.using("", ddrObligationState.getType() == DDRObligationType.PLEDGE);
               require.using("", ddrObligationState.getStatus() == DDRObligationStatus.REQUEST);
                return null;
            });

            final StateAndRef<DDRObligationState> ddrObligationStateStateAndRef = inputStateAndRefs.get(0);
            final DDRObligationState ddrObligationState = inputStateAndRefs.get(0).getState().getData();

            final Party ownerBank = ddrObligationState.getOwner();

            final DDRObjectState ddrObjectState = new DDRObjectState(getOurIdentity(), new Date(), ddrObligationState.getAmount()
            , ddrObligationState.getAmount().getToken(), ddrObligationState.getOwner());

            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), ownerBank.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(ddrObligationStateStateAndRef.getState().getNotary())
                    .addInputState(ddrObligationStateStateAndRef)
                    .addOutputState(ddrObligationState.approveRequest(), DDRObligationContract.ID)
                    .addOutputState(ddrObjectState, DDRObjectContract.ID)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge(), requiredSigners);

            txBuilder.verify(getServiceHub());

            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final FlowSession ownerBankSession = initiateFlow(ownerBank);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableList.of(ownerBankSession),
                    CollectSignaturesFlow.Companion.tracker()));

            SignedTransaction finalityTx = subFlow(new FinalityFlow(fullySignedTx, ImmutableList.of(ownerBankSession)));
            return finalityTx;
        }
    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(com.octo.flows.ApproveDDRPledge.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Responder flow logic goes here.
            final SecureHash txId = subFlow(new ApproveDDRPledge.Responder.CheckTransactionAndSignFlow(counterpartySession, SignTransactionFlow.Companion.tracker())).getId();

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
