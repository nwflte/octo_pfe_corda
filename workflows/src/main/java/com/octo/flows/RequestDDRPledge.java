package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.security.SignatureException;
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
            final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
            final FlowSession centralBankSession = initiateFlow(centralBank);

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));

            String externalId = getOurIdentity().toString().concat(" Pledge " + requesterDate.toString());
            DDRObligationState ddrObligationState = new DDRObligationState(centralBank, getOurIdentity(), requesterDate,
                    amount, getOurIdentity(), DDRObligationType.PLEDGE, DDRObligationStatus.REQUEST, externalId);

            final List<PublicKey> requiredSigners = ImmutableList.of(centralBank.getOwningKey(), getOurIdentity().getOwningKey());

            txBuilder.addOutputState(ddrObligationState, DDRObligationContract.ID)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.RequestDDRPledge(), requiredSigners);

            txBuilder.verify(getServiceHub());

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableList.of(centralBankSession)
                    , CollectSignaturesFlow.Companion.tracker()));


            return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(centralBankSession)));
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

            return subFlow(new ReceiveFinalityFlow(counterPartySession, txId));
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
