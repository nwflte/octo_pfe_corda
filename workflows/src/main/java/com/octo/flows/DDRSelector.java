package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.states.DDRObjectState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.utilities.NonEmptySet;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;

public class DDRSelector {

    // TODO : Get config from configuration file

    private static final int MAX_RETRIES_DEFAULT = 8;
    private static final int RETRY_SLEEP_DEFAULT = 100;
    private static final int RETRY_CAP_DEFAULT = 2000;
    private static final int PAGE_SIZE_DEFAULT = 200;

    private int maxRetries;
    private Integer retrySleep;
    private int retryCap;
    private int pageSize;
    private final ServiceHub serviceHub;

    public DDRSelector(ServiceHub serviceHub) {
        maxRetries = MAX_RETRIES_DEFAULT;
        retrySleep = RETRY_SLEEP_DEFAULT;
        retryCap = RETRY_CAP_DEFAULT;
        pageSize = PAGE_SIZE_DEFAULT;
        this.serviceHub = serviceHub;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setRetrySleep(int retrySleep) {
        this.retrySleep = retrySleep;
    }

    public void setRetryCap(int retryCap) {
        this.retryCap = retryCap;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    private boolean executeQuery(Amount requiredAmount, QueryCriteria criteria, UUID lockId, List<StateAndRef<DDRObjectState>> stateAndRefs) {
        long requiredQuantity = requiredAmount.getQuantity();
        if (requiredQuantity == 0) return false;

        long claimedAmount = 0;
        int pageNumber = DEFAULT_PAGE_NUM;
        PageSpecification pageSpec /*= new PageSpecification()*/;
        Vault.Page<DDRObjectState> results;
        do {
            pageSpec = new PageSpecification(pageNumber, pageSize);
            results = serviceHub.getVaultService().queryBy(DDRObjectState.class, criteria, pageSpec);

            for (StateAndRef<DDRObjectState> state : results.getStates()) {
                stateAndRefs.add(state);
                claimedAmount += state.getState().getData().getAmount().getQuantity();
                if (claimedAmount >= requiredQuantity) break;
            }
            pageNumber++;
        } while (claimedAmount < requiredQuantity && (pageSpec.getPageSize() * (pageNumber - 1)) <= results.getTotalStatesAvailable());

        if (stateAndRefs.isEmpty()) return false;
        if (claimedAmount < requiredQuantity) return false;
        serviceHub.getVaultService().softLockRelease(lockId, NonEmptySet.copyOf(stateAndRefs.stream().map(sar -> sar.getRef()).collect(Collectors.toList())));
        return true;
    }

    @Suspendable
    public List<StateAndRef<DDRObjectState>> selectDDRs(Party owner, UUID lockId, Amount requiredAmount) throws FlowException {

        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria()
                .withSoftLockingCondition(new QueryCriteria.SoftLockingCondition(QueryCriteria.SoftLockingType.UNLOCKED_ONLY, Collections.emptyList()))
                .withParticipants(Collections.singletonList(owner));
//.withExactParticipants(Arrays.asList(getOurIdentity(), ownerBank));

        List<StateAndRef<DDRObjectState>> stateAndRefs = new ArrayList<>();
        for (int retryCount = 1; retryCount <= maxRetries; retryCount++) {
            if (executeQuery(requiredAmount, criteria, lockId, stateAndRefs)) break;
            // TODO: Need to specify exactly why it fails.

            if (retryCount == maxRetries)
                throw new InsufficientBalanceException("Insufficient spendable states identified for " + requiredAmount);

            stateAndRefs.clear();
            int durationMillis = Collections.min(Arrays.asList(retrySleep << retryCount, retryCap / 2)) * (int) (1.0 + Math.random());
            FlowLogic.sleep(Duration.ofMillis(durationMillis));
        }
        return stateAndRefs;
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
}
