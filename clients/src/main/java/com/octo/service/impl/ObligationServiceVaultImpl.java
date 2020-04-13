package com.octo.service.impl;

import com.octo.exceptions.NegativeOrNullAmountException;
import com.octo.flows.*;
import com.octo.mapper.StateMapper;
import com.octo.schemas.PersistentDDRObligation;
import com.octo.service.ObligationService;
import com.octo.states.DDRObligationState;
import com.octo.utils.QueryUtils;
import com.octo.web.NodeRPCConnection;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.octo.CONSTANTS.MAD;

@Service
public class ObligationServiceVaultImpl implements ObligationService {

    private final CordaRPCOps proxy;

    public ObligationServiceVaultImpl(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @Override
    public List<PersistentDDRObligation> loadAll(Vault.StateStatus stateStatus) {
        List<StateAndRef<DDRObligationState>> statesAndRefs = proxy.vaultQueryByCriteria(QueryUtils.withStatusStates(stateStatus),
                DDRObligationState.class).getStates();
        return StateMapper.map(statesAndRefs, PersistentDDRObligation.class);
    }

    @Override
    public Optional<PersistentDDRObligation> findById(String id) {
        List<StateAndRef<DDRObligationState>> list = proxy.vaultQueryByCriteria(QueryUtils.linearStateByExternalId(id),
                DDRObligationState.class).getStates();
        return !list.isEmpty() ? Optional.of((PersistentDDRObligation) StateMapper.map(list.get(0))) : Optional.empty();
    }

    @Override
    public SignedTransaction createPledge(long amount) throws ExecutionException, InterruptedException {
        if (amount <= 0) throw new NegativeOrNullAmountException(amount);
        return proxy
                .startTrackedFlowDynamic(RequestDDRPledge.Initiator.class, new Amount<>(amount, MAD), new Date())
                .getReturnValue().get();
    }

    @Override
    public SignedTransaction cancelPledge(String externalId) throws ExecutionException, InterruptedException {
        return proxy.startFlowDynamic(CancelDDRPledge.Initiator.class, externalId).getReturnValue().get();
    }

    @Override
    public SignedTransaction denyPledge(String externalId) throws ExecutionException, InterruptedException {
        return proxy.startFlowDynamic(DenyDDRPledge.Initiator.class, externalId).getReturnValue().get();
    }

    @Override
    public SignedTransaction approvePledge(String externalId) throws ExecutionException, InterruptedException {
        return proxy.startFlowDynamic(ApproveDDRPledge.Initiator.class, externalId).getReturnValue().get();
    }

    @Override
    public SignedTransaction createRedeem(long amount) throws ExecutionException, InterruptedException {
        if (amount <= 0) throw new NegativeOrNullAmountException(amount);
        return proxy
                .startTrackedFlowDynamic(RequestDDRRedeem.Initiator.class, new Amount<>(amount, MAD), new Date())
                .getReturnValue().get();
    }

    @Override
    public SignedTransaction cancelRedeem(String externalId) throws ExecutionException, InterruptedException {
        return proxy.startFlowDynamic(CancelDDRRedeem.Initiator.class, externalId).getReturnValue().get();
    }

    @Override
    public SignedTransaction denyRedeem(String externalId) throws ExecutionException, InterruptedException {
        return proxy.startFlowDynamic(DenyDDRRedeem.Initiator.class, externalId).getReturnValue().get();
    }

    @Override
    public SignedTransaction approveRedeem(String externalId) throws ExecutionException, InterruptedException {
        return proxy.startFlowDynamic(ApproveDDRRedeem.Initiator.class, externalId).getReturnValue().get();
    }

    @Override
    public String testHttp(String rib) throws ExecutionException, InterruptedException {
        return proxy.startFlowDynamic(TestOracleFlow.class, rib).getReturnValue().get();
    }

}
