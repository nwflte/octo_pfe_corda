package com.octo.flows;

import com.octo.enums.DDRObligationType;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.CollectSignaturesFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Util {

    public static String CENTRAL_BANK_NAME = "O=CentralBank,L=New York,C=US";

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

    public static String generateReference(DDRObligationType type){
        return type + UUID.randomUUID().toString();
    }
}
