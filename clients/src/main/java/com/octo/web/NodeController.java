package com.octo.web;

import com.octo.service.NodeService;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NodeInfo;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("api/node")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @GetMapping("/me")
    public NodeInfo whoami() {
        return nodeService.whoiam();
    }

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping("peers")
    public List<CordaX500Name> getPeers() {
        return nodeService.getPeers();
    }

    @GetMapping("/legalIds")
    public List<Party> myLegalIdentities() {
        return nodeService.myLegalIdentities();
    }

    @GetMapping("/addresses")
    public List<NetworkHostAndPort> myAddresses() {
        return nodeService.myAddresses();
    }

    @GetMapping("/platform-version")
    public int platformVersion() {
        return nodeService.platformVersion();
    }

    @GetMapping("node-time")
    public Instant currentNodeTime() {
        return nodeService.currentNodeTime();
    }

    @GetMapping("notaries")
    public List<Party> notaryIdentities() {
        return nodeService.notaryIdentities();
    }

    @GetMapping("network-map")
    public List<NodeInfo> networkMapSnapshot() {
        return nodeService.networkMapSnapshot();
    }

    @GetMapping("registered-flows")
    public List<String> registeredFlows() {
        return nodeService.registeredFlows();
    }
}
