package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.states.DDRObligationState;
import com.octo.states.DDRObligationStateBuilder;
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

public class ApproveDDRPledge {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        // Idealy, we would pass the externalId as only parameter
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
            final StateAndRef<DDRObligationState> inputStateAndRef = Utils.getObligationByExternalId(externalId, getServiceHub());

            TransactionBuilder txBuilder = approvePledgeTx(inputStateAndRef);

            final FlowSession requesterBankSession = initiateRequesterFlowSession(inputStateAndRef);

            SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub(), requesterBankSession));

            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(requesterBankSession), StatesToRecord.ALL_VISIBLE));
        }

        @Suspendable
        private FlowSession initiateRequesterFlowSession(StateAndRef<DDRObligationState> inputStateAndRef){
            return initiateFlow(inputStateAndRef.getState().getData().getRequester());
        }

        private TransactionBuilder approvePledgeTx(StateAndRef<DDRObligationState> inputStateAndRef){
            final DDRObligationState inputPledge = inputStateAndRef.getState().getData();
            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), inputPledge.getRequester().getOwningKey());
            TransactionBuilder txBuilder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(new DDRObligationStateBuilder(inputPledge).status(DDRObligationStatus.APPROVED).build())
                    .addCommand(new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge(), requiredSigners);

            UtilsDDR.produceDDRObjects(inputPledge).forEach(txBuilder::addOutputState);
            return txBuilder;
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
