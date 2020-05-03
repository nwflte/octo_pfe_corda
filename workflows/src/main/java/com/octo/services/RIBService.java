package com.octo.services;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.flows.SyncIdentitiesFlow;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.Party;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@CordaService
public class RIBService  extends SingletonSerializeAsToken {
    public static final Logger logger = LoggerFactory.getLogger(RIBService.class);

    public final String ourOrgName;

    private final AppServiceHub serviceHub;
    private final Map<String, Party> identities = new HashMap<>();

    public RIBService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        this.ourOrgName = serviceHub.getMyInfo().getLegalIdentities().get(0).getName().getOrganisation();
    }

    public String getOurIdentifier(){
        // TODO This must be a constant, or retrieved from config file, but now the class it's used for all nodes so we do ifs
        if(ourOrgName.equals("BankA")) return "007";
        if(ourOrgName.equals("CentralBank")) return "";
        if(ourOrgName.equals("Notary Service")) return "";
        return "008";
    }

    public Party getPartyFromRIB(String rib){
        String identifier = rib.substring(0, 3);
        return identities.get(identifier);
    }

    public void addToMap(String identifier, Party bank){
        identities.put(identifier, bank);
        logger.info("Identifier {} added to map {}", identifier, identities);
    }
}
