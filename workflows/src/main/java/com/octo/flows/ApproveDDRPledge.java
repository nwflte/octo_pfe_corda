package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.DDRObligationState;
import com.octo.states.DDRObligationStateBuilder;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

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

            // Initiator flow logic goes here.
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
                    .withExternalId(Collections.singletonList(externalId));
            List<StateAndRef<DDRObligationState>> inputStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(DDRObligationState.class, queryCriteria).getStates();

            if (inputStateAndRefs.size() != 1)
                throw new FlowException("There are " + inputStateAndRefs.size() + " states with externalId: " + externalId);

            final StateAndRef<DDRObligationState> inputStateAndRef = inputStateAndRefs.get(0);
            final DDRObligationState inputPledge = inputStateAndRef.getState().getData();

            final Party ownerBank = inputPledge.getOwner();
            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), ownerBank.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(new DDRObligationStateBuilder(inputPledge).status(DDRObligationStatus.APPROVED).build(),
                            DDRObligationContract.ID)
                    .addCommand(new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge(), requiredSigners);

            IssueTokensUtilitiesKt.addIssueTokens(txBuilder, produceDDRObjects(inputPledge));

            /*List<FungibleToken> produced = produceDDRObjects(inputPledge);

            // TODO Case where list size is 0
            txBuilder.addCommand(new IssueTokenCommand(new IssuedTokenType(getOurIdentity(), FiatCurrency.Companion.getInstance("MAD")),
                    IntStream.range(1, produced.size()+1).boxed().collect(Collectors.toList())) , requiredSigners);

            produceDDRObjects(inputPledge).forEach(ddr -> txBuilder.addOutputState(ddr, FungibleTokenContract.Companion.getContractId()));*/

            txBuilder.verify(getServiceHub());

            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final FlowSession ownerBankSession = initiateFlow(ownerBank);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Collections.singletonList(ownerBankSession),
                    CollectSignaturesFlow.Companion.tracker()));

            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(ownerBankSession), StatesToRecord.ALL_VISIBLE));
        }

        /**
         * Produces DDR Objects of 10DH, then if there's a rest it produces additional one of rest value
         * Example: For quantity 2500 of tokens (That is 15DH), will produce 3 DDRObjects, two of 10DH and other of 5DH.
         *
         * @return
         */
        private List<FungibleToken> produceDDRObjects(DDRObligationState obligationState) {
            Amount<Currency> amount = obligationState.getAmount();
            long quantity = amount.getQuantity(); // For exmaple 1000 token
            DDRObjectStateBuilder builder = new DDRObjectStateBuilder();
            builder.issuer(obligationState.getIssuer()).owner(obligationState.getOwner())
                    .currency(obligationState.getCurrency());
            if (quantity > 1000) {
                int numberOfTokens = (int) Math.ceil((double) quantity / 1000);
                return amount.splitEvenly(numberOfTokens).stream().map(am -> builder.amount(am.getQuantity()/100).build()).collect(Collectors.toList());
            }

            return Collections.singletonList(builder.amount(quantity/100).build());
        }
    }


    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(Initiator.class)
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
