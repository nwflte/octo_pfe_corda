package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObligationState;
import com.octo.states.DDRObligationStateBuilder;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.SoftLockingType;
import net.corda.core.node.services.vault.QueryCriteria.SoftLockingCondition;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.NonEmptySet;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
            // Initiator flow logic goes here.
            VaultService vaultService = getServiceHub().getVaultService();

            QueryCriteria criteriaObligation = new LinearStateQueryCriteria().withExternalId(Collections.singletonList(externalId));
            final StateAndRef<DDRObligationState> obligationStateAndRef = vaultService
                    .queryBy(DDRObligationState.class, criteriaObligation).getStates().get(0);
            final DDRObligationState obligationState = obligationStateAndRef.getState().getData();
            final Party ownerBank = obligationState.getOwner();

            QueryCriteria vaultCriteria = new VaultQueryCriteria()
                    .withSoftLockingCondition(new SoftLockingCondition(SoftLockingType.UNLOCKED_ONLY, Collections.EMPTY_LIST))
            .withExactParticipants(Arrays.asList(getOurIdentity(), ownerBank));
            // Doesn't get states
            /*
            QueryCriteria queryDDRAsset = new FungibleAssetQueryCriteria().withRelevancyStatus(Vault.RelevancyStatus.ALL)
                    .withOwner(Collections.singletonList(ownerBank));

            List<StateAndRef<DDRObjectState>> toArchiveDDR = vaultService
                    .tryLockFungibleStatesForSpending(getRunId().getUuid(), queryDDRAsset, obligationState.getAmount(),DDRObjectState.class);
*/
            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), ownerBank.getOwningKey());

            List<StateAndRef<DDRObjectState>> toArchiveDDR = vaultService.queryBy(DDRObjectState.class,vaultCriteria).getStates();

            vaultService.softLockRelease(getRunId().getUuid(), NonEmptySet.copyOf(toArchiveDDR.stream()
                    .map(StateAndRef::getRef).collect(Collectors.toList())));

            TransactionBuilder txBuilder = new TransactionBuilder(obligationStateAndRef.getState().getNotary())
                    .addInputState(obligationStateAndRef)
                    .addOutputState(new DDRObligationStateBuilder(obligationState).status(DDRObligationStatus.APPROVED).build())
                    .addCommand(new DDRObligationContract.DDRObligationCommands.ApproveDDRRedeem(), requiredSigners);

            addSufficientDDRToTransaction(txBuilder, toArchiveDDR, obligationState.getAmount().getQuantity());

            txBuilder.verify(getServiceHub());
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            final FlowSession ownerBankSession = initiateFlow(ownerBank);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Collections.singletonList(ownerBankSession),
                    CollectSignaturesFlow.Companion.tracker()));
            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(ownerBankSession), StatesToRecord.ALL_VISIBLE));
        }

        private void addSufficientDDRToTransaction(TransactionBuilder txBuilder, List<StateAndRef<DDRObjectState>> queriedDDRs, long amount){
            AtomicLong summed = new AtomicLong();
            Long sum;
            queriedDDRs.forEach(ddr -> {
                if ( summed.addAndGet(ddr.getState().getData().getAmount().getQuantity()) >= amount) return;
                txBuilder.addInputState(ddr);
            });
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
