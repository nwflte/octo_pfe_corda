package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.IntraBankTransferContract;
import com.octo.states.IntraBankTransferState;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.*;

public class RecordIntraBankTransfer {

    // ******************
    // * Initiator flow *
    // ******************
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String senderRIB;
        private final String receiverRIB;
        private final Amount<Currency> amount;
        private final Date executionDate;

        private final ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(Amount<Currency> amount, String senderRIB, String receiverRIB, Date executionDate) {
            this.amount = amount;
            this.senderRIB = senderRIB;
            this.receiverRIB = receiverRIB;
            this.executionDate = executionDate;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
            assert centralBank != null;
            final FlowSession centralBankSession = initiateFlow(centralBank);

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));

            IntraBankTransferState transferState = new IntraBankTransferState(senderRIB, receiverRIB, getOurIdentity(), amount,
                    executionDate, executionDate + senderRIB.substring(0, 5) + receiverRIB.substring(0, 5));

            final List<PublicKey> requiredSigners = Arrays.asList(centralBank.getOwningKey(), getOurIdentity().getOwningKey());

            txBuilder.addOutputState(transferState)
                    .addCommand(new IntraBankTransferContract.IntraBankTransferCommands.RecordTransfer(), requiredSigners);

            txBuilder.verify(getServiceHub());

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Collections.singletonList(centralBankSession)
                    , CollectSignaturesFlow.Companion.tracker()));

            return subFlow(new FinalityFlow(fullySignedTx, centralBankSession));
        }

    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(RecordIntraBankTransfer.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterPartySession;

        public Responder(FlowSession counterPartySession) {
            this.counterPartySession = counterPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SecureHash txId = subFlow(new CheckTransactionAndSignFlow(counterPartySession, SignTransactionFlow.Companion.tracker())).getId();
            return subFlow(new ReceiveFinalityFlow(counterPartySession, txId, StatesToRecord.ALL_VISIBLE));
        }

        private class CheckTransactionAndSignFlow extends SignTransactionFlow {

            public CheckTransactionAndSignFlow(@NotNull FlowSession otherSideSession, @NotNull ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                try {
                    LedgerTransaction ltx = stx.toLedgerTransaction(getServiceHub(), false);
                } catch (SignatureException e) {
                    throw new FlowException("Transaction had invalid signature.");
                }
            }
        }

    }


}
