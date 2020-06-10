package com.octo.service.impl;

import com.octo.service.DDRObjectService;
import com.octo.states.DDRObjectState;
import com.octo.utils.QueryUtils;
import com.octo.web.NodeRPCConnection;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DDRObjectServiceVaultImpl implements DDRObjectService {

    private final CordaRPCOps proxy;

    public DDRObjectServiceVaultImpl(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    // TODO Aggregate related stats into one
    @Override
    public long balance() {
        List<StateAndRef<DDRObjectState>> states = proxy.vaultQueryBy(QueryUtils.withStatusStates(Vault.StateStatus.UNCONSUMED),
                new PageSpecification(1, 1000),
                new Sort(Collections.emptyList()),
                DDRObjectState.class
                ).getStates();
        return states.stream()
                .mapToLong(stateAndRef -> stateAndRef.getState().getData().getAmount().getQuantity())
                .sum();
    }

    @Override
    public long count() {
        return proxy.vaultQueryBy(QueryUtils.withStatusStates(Vault.StateStatus.UNCONSUMED),
                new PageSpecification(1, 1000),
                new Sort(Collections.emptyList()),
                DDRObjectState.class
        ).getStates().size();
    }

    @Override
    public double average() {
        return proxy.vaultQueryBy(QueryUtils.withStatusStates(Vault.StateStatus.UNCONSUMED),
                new PageSpecification(1, 1000),
                new Sort(Collections.emptyList()),
                DDRObjectState.class)
                .getStates().stream()
                .mapToLong(stateAndRef -> stateAndRef.getState().getData().getAmount().getQuantity())
                .average().orElseThrow(() -> new IllegalStateException("No DDR found"));
    }
}
