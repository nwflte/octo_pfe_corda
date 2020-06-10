package com.octo.service;

import com.octo.web.NodeRPCConnection;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.octo.CONSTANTS.SERVICE_NAMES;

@Service
public class NodeService {

    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;

    public NodeService(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = rpc.proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    public NodeInfo whoiam() {
        return proxy.nodeInfo();
    }

    public List<CordaX500Name> getPeers() {
        List<NodeInfo> nodeInfo = proxy.networkMapSnapshot();
        return nodeInfo.stream().map(n -> n.getLegalIdentities().get(0).getName()).filter(
                name -> !myLegalName.getOrganisation().contains(name.getOrganisation()) && !SERVICE_NAMES.contains(name.getOrganisation())
        ).collect(Collectors.toList());
    }

    public List<Party> myLegalIdentities() {
        return proxy.nodeInfo().getLegalIdentities();
    }

    public List<NetworkHostAndPort> myAddresses() {
        return proxy.nodeInfo().getAddresses();
    }

    public int platformVersion() {
        return proxy.nodeInfo().getPlatformVersion();
    }

    public Instant currentNodeTime() {
        return proxy.currentNodeTime();
    }

    public List<Party> notaryIdentities() {
        return proxy.notaryIdentities();
    }

    public List<NodeInfo> networkMapSnapshot() {
        return proxy.networkMapSnapshot();
    }

    public List<String> registeredFlows() {
        return proxy.registeredFlows();
    }

    public Party getPartyFromRIB(String rib) {
        return proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=BankB,L=New York,C=US"));
    }

}
