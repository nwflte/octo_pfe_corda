package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.InterBankTransferContract;
import com.octo.states.InterBankTransferState;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.List;

public class AtomicExchangeDDR {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String senderRIB;
        private final String receiverRIB;
        private final Party receiverBank;
        private final Amount<Currency> amount;
        private final Date executionDate;
        private final ProgressTracker progressTracker = new ProgressTracker();
        private Party centralBank;

        public Initiator(String senderRIB, String receiverRIB, Party receiverBank, Amount<Currency> amount, Date executionDate) {
            this.senderRIB = senderRIB;
            this.receiverRIB = receiverRIB;
            this.receiverBank = receiverBank;
            this.amount = amount;
            this.executionDate = executionDate;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Initiator flow logic goes here.
            VaultService vaultService = getServiceHub().getVaultService();
            centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));

            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), centralBank.getOwningKey(), receiverBank.getOwningKey());

            InterBankTransferState transferState = new InterBankTransferState(senderRIB, receiverRIB, getOurIdentity(),
                    receiverBank, amount, executionDate, executionDate + senderRIB.substring(0, 5) + receiverRIB.substring(0, 5));

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addOutputState(transferState)
                    .addCommand(new InterBankTransferContract.InterBankTransferCommands.BankTransfer(), requiredSigners);

            MoveTokensUtilitiesKt.addMoveFungibleTokens(txBuilder, getServiceHub(),
                    new Amount<>(amount.getQuantity(), FiatCurrency.Companion.getInstance("MAD")),
                    receiverBank, getOurIdentity());

            txBuilder.verify(getServiceHub());

            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final FlowSession centralBankSession = initiateFlow(centralBank);
            final FlowSession receiverBankSession = initiateFlow(receiverBank);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Arrays.asList(centralBankSession, receiverBankSession),
                    CollectSignaturesFlow.Companion.tracker()));
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(centralBankSession, receiverBankSession), StatesToRecord.ALL_VISIBLE));
        }

    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(Initiator.class)
    public static class CentralBankResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterpartySession;

        public CentralBankResponder(FlowSession counterpartySession) {
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


    // ******************
    // * Responder flow *
    // ******************
    //@InitiatedBy(com.octo.flows.AtomicExchangeDDR.Initiator.class)
    public static class BankResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterpartySession;

        public BankResponder(FlowSession counterpartySession) {
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
