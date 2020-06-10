package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObligationState;
import com.octo.utils.Utils;
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

public class RequestDDRRedeem {

    // ******************
    // * Initiator flow *
    // ******************
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final Amount<Currency> amount;

        private final ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(Amount<Currency> amount) {
            this.amount = amount;
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

            final TransactionBuilder txBuilder = requestRedeemTx(amount, centralBankParty);

            final SignedTransaction fullySignedTx = subFlow(Utils.verifyAndCollectSignatures(txBuilder, getServiceHub(), centralBankSession));

            return subFlow(new FinalityFlow(fullySignedTx, centralBankSession));
        }

        private TransactionBuilder requestRedeemTx(Amount<Currency> amount, Party centralBank) {
            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));
            final List<PublicKey> requiredSigners = Arrays.asList(centralBank.getOwningKey(), getOurIdentity().getOwningKey());

            DDRObligationState redeemState = new DDRObligationState(centralBank, getOurIdentity(), new Date(), amount,
                    getOurIdentity(), DDRObligationType.REDEEM, DDRObligationStatus.REQUEST, Utils.generateReference("REDEEM"));

            return txBuilder.addOutputState(redeemState)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.RequestDDRRedeem(), requiredSigners);
        }

    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(com.octo.flows.RequestDDRRedeem.Initiator.class)
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
