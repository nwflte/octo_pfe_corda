package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.states.DDRObligationState;
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
import java.util.List;

public class CancelDDRPledge {

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

            final TransactionBuilder txBuilder = cancelPledgeTx(requestStateAndRef);

            final FlowSession issuerSession = initiateIssuerFlowSession(requestStateAndRef);

            SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub() ,issuerSession));

            return subFlow(new FinalityFlow(fullySignedTx, issuerSession));
        }

        private TransactionBuilder cancelPledgeTx(StateAndRef<DDRObligationState> inputStateAndRef) {
            final DDRObligationState inputState = inputStateAndRef.getState().getData();

            final List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), inputState.getIssuer().getOwningKey());
            return new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.CancelDDRPledge(), requiredSigners);
        }

        @Suspendable
        private FlowSession initiateIssuerFlowSession(StateAndRef<DDRObligationState> inputStateAndRef){
            return initiateFlow(inputStateAndRef.getState().getData().getIssuer());
        }
    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(com.octo.flows.CancelDDRPledge.Initiator.class)
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
            return subFlow(new ReceiveFinalityFlow(counterpartySession, txId, StatesToRecord.ALL_VISIBLE));
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
