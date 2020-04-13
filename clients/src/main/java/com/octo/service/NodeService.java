package com.octo.service;

import com.google.common.collect.ImmutableMap;
import com.octo.web.NodeRPCConnection;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.octo.CONSTANTS.SERVICE_NAMES;

@Service
public class NodeService {

    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;

    public NodeService(NodeRPCConnection rpc){
        this.proxy = rpc.proxy;
        this.myLegalName = rpc.proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    public ImmutableMap<String, CordaX500Name> whoiam(){
        return ImmutableMap.of("me", myLegalName);
    }

    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfo = proxy.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfo.stream().map(n -> n.getLegalIdentities().get(0).getName()).filter(
                name -> !myLegalName.getOrganisation().contains(name.getOrganisation()) && !SERVICE_NAMES.contains(name.getOrganisation())
        ).collect(Collectors.toList()));
    }

    public Party getPartyFromRIB(String rib){
        return proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=BankB,L=New York,C=US"));
    }

}
