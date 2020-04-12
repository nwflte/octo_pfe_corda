package com.octo.web;

import com.google.common.collect.ImmutableMap;
import com.octo.service.NodeService;
import net.corda.core.identity.CordaX500Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController("api/node")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @GetMapping(value = "/me")
    public ImmutableMap<String, CordaX500Name> whoami() {
        return nodeService.whoiam();
    }

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = "peers", produces = APPLICATION_JSON_VALUE)
    public Map<String, List<CordaX500Name>> getPeers() {
        return nodeService.getPeers();
    }
}
