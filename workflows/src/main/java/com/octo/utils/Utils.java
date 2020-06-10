package com.octo.utils;

import co.paralleluniverse.fibers.Suspendable;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.*;
import net.corda.core.flows.CollectSignaturesFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.*;

public class Utils {

    public static final String CENTRAL_BANK_NAME = "O=CentralBank,L=New York,C=US";

    public static Party getCentralBankParty(ServiceHub serviceHub) throws FlowException {
        final Party centralBank = serviceHub.getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse(CENTRAL_BANK_NAME));
        if(centralBank == null)
            throw new FlowException("Could not find Central Bank Party with name " + CENTRAL_BANK_NAME);
        return centralBank;
    }

    public static FlowLogic<SignedTransaction> verifyAndCollectSignatures(TransactionBuilder txBuilder,ServiceHub serviceHub ,FlowSession ...signers)
            throws TransactionResolutionException, TransactionVerificationException, AttachmentResolutionException {

        txBuilder.verify(serviceHub);
        final SignedTransaction partSignedTx = serviceHub.signInitialTransaction(txBuilder);
        return new CollectSignaturesFlow(partSignedTx, Arrays.asList(signers),CollectSignaturesFlow.Companion.tracker());
    }

    public static StateAndRef<DDRObligationState> getObligationByExternalId(String externalId, ServiceHub serviceHub) throws FlowException {
        QueryCriteria criteria = new LinearStateQueryCriteria().withExternalId(Collections.singletonList(externalId));
        List<StateAndRef<DDRObligationState>> stateAndRefs = serviceHub.getVaultService()
                .queryBy(DDRObligationState.class, criteria).getStates();
        if(stateAndRefs.isEmpty()) throw new FlowException("No obligation is found with external id " + externalId);
        return stateAndRefs.get(0);
    }

    @Suspendable
    public static List<StateAndRef<DDRObjectState>> selectDDRs(Party owner, Amount<Currency> amount, ServiceHub serviceHub, UUID lockId) throws FlowException {
        DDRSelector selector = new DDRSelector(serviceHub);
        return selector.selectDDRs(owner, lockId, amount);
    }

    public static String generateReference(String type){
        return type + UUID.randomUUID().toString();
    }
}
