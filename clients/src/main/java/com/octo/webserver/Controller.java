package com.octo.webserver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.octo.flows.RequestDDRPledge;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.*;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("api/obligations") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private static final String CENTRAL_BANK_NAME = "O=CentralBank,L=New York,C=US";
    private static List<String> SERVICE_NAMES =  Arrays.asList("Notary", "Network Map Service");
    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = rpc.proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }

    @GetMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    private ImmutableMap<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value ="peers", produces = APPLICATION_JSON_VALUE)
    private Map<String, List<CordaX500Name>> getPeers(){
        List<NodeInfo> nodeInfo = proxy.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfo.stream().map(n -> n.getLegalIdentities().get(0).getName()).filter(
           name ->  !myLegalName.getOrganisation().contains(name.getOrganisation()) && !SERVICE_NAMES.contains(name)
        ).collect(Collectors.toList()));
    }

    /**
     * Displays all ddr Obligations states that exist in the node's vault.
     */
    @GetMapping(value = "obligations", produces = APPLICATION_JSON_VALUE)
    private ResponseEntity<List<StateAndRef<DDRObligationState>>> getObligations() {
        return ResponseEntity.ok(proxy.vaultQuery(DDRObligationState.class).getStates());
    }

    @PostMapping(value = "create-obligation"/*, produces = TEXT_PLAIN_VALUE , consumes = APPLICATION_FORM_URLENCODED_VALUE*/)
    private ResponseEntity<String> createObligation( @RequestParam Map<String, String> request) {
        long amount = Long.valueOf(request.get("amount"));

        if (amount <= 0 ) {
            return ResponseEntity.badRequest().body("Query parameter 'amount' must be non-negative.\n");
        }

        CordaX500Name partyX500Name = CordaX500Name.parse(CENTRAL_BANK_NAME);
        Party otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name);
        if(otherParty == null)
            return ResponseEntity.badRequest().body("Party named " + CENTRAL_BANK_NAME + " cannot be found.\n");

        try {
            SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(RequestDDRPledge.Initiator.class, new Amount<Currency>(amount, Currency.getInstance("MAD")), new Date())
                    .getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + signedTx.getId() +  " committed to ledger.\n");

        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Displays all Obligation states that only this node has been involved in.
     */
    @GetMapping(value = "my-obligations", produces = APPLICATION_JSON_VALUE)
    private ResponseEntity<List<StateAndRef<DDRObligationState>>> getMyObligationss()  {
        List<StateAndRef<DDRObligationState>> myDDRObs =
                proxy.vaultQuery(DDRObligationState.class).getStates().stream().filter(
                        s -> s.getState().getData().getOwner().equals(proxy.nodeInfo().getLegalIdentities().get(0)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(myDDRObs);
    }
}