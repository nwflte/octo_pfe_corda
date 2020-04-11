package com.octo.service.impl;

import com.octo.QueryUtils;
import com.octo.exceptions.NegativeOrNullAmountException;
import com.octo.flows.CancelDDRPledge;
import com.octo.flows.CancelDDRRedeem;
import com.octo.flows.RequestDDRPledge;
import com.octo.flows.RequestDDRRedeem;
import com.octo.mapper.StateMapper;
import com.octo.schemas.PersistentDDRObligation;
import com.octo.service.ObligationService;
import com.octo.states.DDRObligationState;
import com.octo.web.NodeRPCConnection;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.transactions.SignedTransaction;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.octo.Constants.MAD;

@Service
public class ObligationServiceVaultImpl implements ObligationService {

    private final CordaRPCOps proxy;

    public ObligationServiceVaultImpl(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @Override
    public Optional<PersistentDDRObligation> testFindPersistentState(String id) {
        DDRObligationState stateAndRef = proxy.vaultQueryByCriteria(QueryUtils.linearStateByExternalId(id),
                DDRObligationState.class).getStates().get(0).getState().getData();

        Iterator<MappedSchema> iterator = stateAndRef.supportedSchemas().iterator();
        MappedSchema mappedSchema = null;
        if(!iterator.hasNext()) return Optional.empty();

        mappedSchema = iterator.next();
        PersistentState entity = stateAndRef.generateMappedObject(mappedSchema);
        PersistentDDRObligation persistentDDRObligation = (PersistentDDRObligation) entity;
        System.out.println(persistentDDRObligation);
        return Optional.of(persistentDDRObligation);
    }

    @Override
    public List<PersistentDDRObligation> loadAll(Vault.StateStatus stateStatus) {
        List<StateAndRef<DDRObligationState>> statesAndRefs = proxy.vaultQueryByCriteria(QueryUtils.withStatusStates(stateStatus),
                DDRObligationState.class).getStates();
        return StateMapper.map(statesAndRefs);
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

}
