package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObligationState;
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

public class RequestDDRPledge {

    // ******************
    // * Initiator flow *
    // ******************
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final Amount<Currency> amount;
        private final Date requesterDate;

        private final ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(Amount<Currency> amount, Date requesterDate) {
            this.amount = amount;
            this.requesterDate = requesterDate;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final Party centralBankParty = Util.getCentralBankParty(getServiceHub());
            final FlowSession centralBankSession = initiateFlow(centralBankParty);

            final TransactionBuilder txBuilder = requestPledgeTx(amount, centralBankParty);

            final SignedTransaction fullySignedTx = subFlow(Util.verifyAndCollectSignatures(txBuilder, getServiceHub(), centralBankSession));

            return subFlow(new FinalityFlow(fullySignedTx, centralBankSession));
        }

        private TransactionBuilder requestPledgeTx(Amount<Currency> amount, Party centralBank) {
            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));
            final List<PublicKey> requiredSigners = Arrays.asList(centralBank.getOwningKey(), getOurIdentity().getOwningKey());

            DDRObligationState ddrObligationState = new DDRObligationState(centralBank, getOurIdentity(), new Date(), amount, getOurIdentity(),
                    DDRObligationType.PLEDGE, DDRObligationStatus.REQUEST, Util.generateReference(DDRObligationType.PLEDGE));
            return txBuilder.addOutputState(ddrObligationState, DDRObligationContract.ID)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.RequestDDRPledge(), requiredSigners);
        }


    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(com.octo.flows.RequestDDRPledge.Initiator.class)
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
