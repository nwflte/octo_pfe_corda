package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObjectStateBuilder;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ApproveDDRRedeem {

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
            final StateAndRef<DDRObligationState> stateAndRef = Utils.getObligationByExternalId(externalId, getServiceHub());

            TransactionBuilder txBuilder = approveRedeemTx(stateAndRef);

            final FlowSession requesterBankSession = initiateRequesterFlowSession(stateAndRef);

            SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub(), requesterBankSession));

            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(requesterBankSession), StatesToRecord.ALL_VISIBLE));
        }

        @Suspendable
        private FlowSession initiateRequesterFlowSession(StateAndRef<DDRObligationState> inputStateAndRef) {
            return initiateFlow(inputStateAndRef.getState().getData().getRequester());
        }

        @Suspendable
        private TransactionBuilder approveRedeemTx(StateAndRef<DDRObligationState> stateAndRef) throws FlowException {
            final DDRObligationState state = stateAndRef.getState().getData();
            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), state.getOwner().getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(stateAndRef.getState().getNotary())
                    .addInputState(stateAndRef)
                    .addOutputState(new DDRObligationStateBuilder(state).status(DDRObligationStatus.APPROVED).build())
                    .addCommand(new DDRObligationContract.DDRObligationCommands.ApproveDDRRedeem(), requiredSigners);

            AtomicLong totalAmountConsumed = new AtomicLong(0L);
            Utils.selectDDRs(state.getOwner(), state.getAmount(), getServiceHub(), txBuilder.getLockId()).forEach(ddr -> {
                txBuilder.addInputState(ddr);
                totalAmountConsumed.addAndGet(ddr.getState().getData().getAmount().getQuantity());
            });
            addRestDDROutput(txBuilder, totalAmountConsumed.get(), state);
            return txBuilder;
        }

        private TransactionBuilder addRestDDROutput(TransactionBuilder txBuilder, long amountConsumed, DDRObligationState obligation) {
            if (amountConsumed <= obligation.getAmount().getQuantity()) return txBuilder;
            long amountToRequired = obligation.getAmount().getQuantity();
            DDRObjectState restDDR = new DDRObjectStateBuilder().owner(obligation.getOwner()).issuerDate(new Date())
                    .issuer(obligation.getIssuer()).currency(obligation.getCurrency()).amount(amountConsumed - amountToRequired).build();
            return txBuilder.addOutputState(restDDR);
        }
    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(com.octo.flows.ApproveDDRRedeem.Initiator.class)
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
