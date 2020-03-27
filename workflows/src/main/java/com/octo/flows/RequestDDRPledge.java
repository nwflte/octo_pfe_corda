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
import net.corda.core.serialization.CordaSerializable;
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

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class RequestDDRPledge {

    @CordaSerializable
    private static class DDRPledgenfo {
        private final Amount<Currency> amount;
        private final Date requesterDate;

        private DDRPledgenfo(Amount<Currency> amount, Date requesterDate) {
            this.amount = amount;
            this.requesterDate = requesterDate;
        }
    }
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
            // Initiator flow logic goes here.
            final DDRPledgenfo ddrPledgenfo = new DDRPledgenfo(amount, requesterDate);

            final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
            final FlowSession centralBankSession = initiateFlow(centralBank);

            centralBankSession.send(ddrPledgenfo);

            final SecureHash txId = subFlow(new CheckTransactionAndSignFlow(centralBankSession, SignTransactionFlow.Companion.tracker())).getId();

            return subFlow(new ReceiveFinalityFlow(centralBankSession, txId));
        }

        private class CheckTransactionAndSignFlow extends SignTransactionFlow {

            public CheckTransactionAndSignFlow(@NotNull FlowSession otherSideSession, @NotNull ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                try {
                    LedgerTransaction ltx = stx.toLedgerTransaction(getServiceHub(), false);
                    DDRObligationState output = (DDRObligationState) ltx.getOutputStates().get(0);
                    requireThat(require -> {
                        require.using("Amount has been changed", output.getAmount().compareTo(amount) == 0);
                        require.using("Requester date has been changed", output.getRequesterDate().compareTo(requesterDate) == 0);
                        return null;
                    });

                } catch (SignatureException e) {
                    throw new FlowException("Transaction had invalid signature.");
                }
            }
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
            // Responder flow logic goes here.
            DDRPledgenfo ddrPledgenfo = counterPartySession.receive(DDRPledgenfo.class).unwrap(info -> {
                // Check
                return info;
            });

            final Party pledgingBankParty = counterPartySession.getCounterparty();

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));

            String externalId = pledgingBankParty.toString().concat(" Pledge" + ddrPledgenfo.requesterDate.toString());
            DDRObligationState ddrObligationState = new DDRObligationState(getOurIdentity(), pledgingBankParty,ddrPledgenfo.requesterDate,
                    ddrPledgenfo.amount, pledgingBankParty, DDRObligationType.PLEDGE, DDRObligationStatus.REQUEST, externalId);

            final List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), pledgingBankParty.getOwningKey());

            txBuilder.addOutputState(ddrObligationState, DDRObligationContract.ID)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.RequestDDRPledge(), requiredSigners);

            txBuilder.verify(getServiceHub());

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            //counterPartySession.send(partSignedTx);

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableList.of(counterPartySession)
            ,CollectSignaturesFlow.Companion.tracker()));


            SignedTransaction finalityTx = subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(counterPartySession)));
            return finalityTx;
        }
    }

}
