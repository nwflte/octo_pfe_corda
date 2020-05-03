package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.services.RIBService;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SyncIdentitiesFlow {

    @InitiatingFlow
    @StartableByRPC
    @StartableByService
    public static class Initiator extends FlowLogic<Void> {

        public static final Logger logger = LoggerFactory.getLogger(Initiator.class);

        @Override
        @Suspendable
        public Void call() throws FlowException {
            final List<NodeInfo> allNodes = getServiceHub().getNetworkMapCache().getAllNodes();
            String ourIdentifier = getServiceHub().cordaService(RIBService.class).getOurIdentifier();
            logger.info("Our Identifier = {}", ourIdentifier);

            for(NodeInfo nodeInfo : allNodes){
                logger.info("Inside foreach of node={}", nodeInfo);
                Party party = nodeInfo.getLegalIdentities().get(0);
                FlowSession session = initiateFlow(party);
                String identifier = null;
                try {
                    logger.info("Sending our identifier and getting other parties identifiers");
                    identifier = session.sendAndReceive(String.class, ourIdentifier)
                            .unwrap(SyncIdentitiesFlow::validator);
                    logger.info("Received identifier={}", identifier);
                } catch (FlowException e) {
                    e.printStackTrace();
                }
                if(identifier != null) getServiceHub().cordaService(RIBService.class).addToMap(identifier, party);
            }
            return null;
        }

    }

    @Suspendable
    public static String validator(String strNum) {
        if (strNum == null || strNum.length() != 3) return null;
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return null;
        }
        return strNum;
    }


    @InitiatedBy(com.octo.flows.SyncIdentitiesFlow.Initiator.class)
    public static class Responder extends FlowLogic<Void> {

        private final FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            String receivedIdentifier = counterpartySession.receive(String.class).unwrap(SyncIdentitiesFlow::validator);
            getServiceHub().cordaService(RIBService.class).addToMap(receivedIdentifier, counterpartySession.getCounterparty());
            String ourIdentifier = getServiceHub().cordaService(RIBService.class).getOurIdentifier();
            counterpartySession.send(ourIdentifier);
            return null;
        }
    }
}
