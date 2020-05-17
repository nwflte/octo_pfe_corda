package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.states.DDRObligationState;
import com.octo.builders.DDRObligationStateBuilder;
import com.octo.utils.Utils;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;
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
            final StateAndRef<DDRObligationState> requestStateAndRef = Utils.getObligationByExternalId(externalId, getServiceHub());

            TransactionBuilder txBuilder = denyPledgeTx(requestStateAndRef);

            final FlowSession requesterSession = initiateRequesterFlowSession(requestStateAndRef);

            SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub(), requesterSession));

            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(requesterSession), StatesToRecord.ALL_VISIBLE));
        }

        private TransactionBuilder denyPledgeTx(StateAndRef<DDRObligationState> inputStateAndRef) {
            final DDRObligationState inputState = inputStateAndRef.getState().getData();

            final List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), inputState.getRequester().getOwningKey());
            return new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(new DDRObligationStateBuilder(inputState).status(DDRObligationStatus.REJECTED).build())
                    .addCommand(new DDRObligationContract.DDRObligationCommands.DenyDDRPledge(), requiredSigners);
        }

        @Suspendable
        private FlowSession initiateRequesterFlowSession(StateAndRef<DDRObligationState> inputStateAndRef){
            return initiateFlow(inputStateAndRef.getState().getData().getRequester());
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
