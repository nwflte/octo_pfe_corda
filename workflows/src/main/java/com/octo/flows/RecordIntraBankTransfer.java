package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.IntraBankTransferContract;
import com.octo.states.IntraBankTransferState;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.List;

public class RecordIntraBankTransfer {

    // ******************
    // * Initiator flow *
    // ******************
    @InitiatingFlow
    @StartableByRPC
    @StartableByService
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String senderRIB;
        private final String receiverRIB;
        private final Amount<Currency> amount;
        private final Date executionDate;
        private String reference = "";

        private final ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(Amount<Currency> amount, String senderRIB, String receiverRIB, Date executionDate, String reference) {
            this.amount = amount;
            this.senderRIB = senderRIB;
            this.receiverRIB = receiverRIB;
            this.executionDate = executionDate;
            this.reference = reference;
        }

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
            final Party centralBankParty = Utils.getCentralBankParty(getServiceHub());
            final FlowSession centralBankSession = initiateFlow(centralBankParty);

            TransactionBuilder txBuilder = recordIntraTx(amount, senderRIB, receiverRIB, centralBankParty);

            final SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub(), centralBankSession));

            return subFlow(new FinalityFlow(fullySignedTx, centralBankSession));
        }

        private TransactionBuilder recordIntraTx(Amount<Currency> amount, String senderRIB, String receiverRIB, Party centralBank) {
            reference = reference.isEmpty() ? Utils.generateReference("INTRA") : reference;
            IntraBankTransferState transferState = new IntraBankTransferState(senderRIB, receiverRIB, getOurIdentity(), amount,
                    executionDate, reference);
            List<PublicKey> requiredSigners = Arrays.asList(centralBank.getOwningKey(), getOurIdentity().getOwningKey());
            return new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addOutputState(transferState)
                    .addCommand(new IntraBankTransferContract.IntraBankTransferCommands.RecordTransfer(), requiredSigners);
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

        private static class CheckTransactionAndSignFlow extends SignTransactionFlow {

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
