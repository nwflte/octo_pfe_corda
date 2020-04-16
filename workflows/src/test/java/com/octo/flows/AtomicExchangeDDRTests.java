package com.octo.flows;

import com.google.common.collect.ImmutableList;
import com.octo.states.DDRObligationState;
import com.octo.utils.DDRUtils;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Currency;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class AtomicExchangeDDRTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
    )));
    private final StartedMockNode a = network.createNode(CordaX500Name.parse("O=BankA,L=New York,C=US"));
    private final StartedMockNode b = network.createNode(CordaX500Name.parse("O=BankB,L=New York,C=US"));
    private final StartedMockNode bc = network.createNode(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
    private String externalId;
    private Amount<Currency> amount3200 = DDRUtils.buildDirham(1500);
    private Amount<Currency> amount1000 =  DDRUtils.buildDirham(500);

    public AtomicExchangeDDRTests() {
        a.registerInitiatedFlow(ApproveDDRPledge.Responder.class);
        bc.registerInitiatedFlow(RequestDDRPledge.Responder.class);
        bc.registerInitiatedFlow(AtomicExchangeDDR.CentralBankResponder.class);
        b.registerInitiatedFlow(AtomicExchangeDDR.CentralBankResponder.class);
    }

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        network.runNetwork();

        RequestDDRPledge.Initiator flowPledge = new RequestDDRPledge.Initiator(amount3200, new Date(new Date().getTime() - 86400000));
        CordaFuture<SignedTransaction> futurePledge = a.startFlow(flowPledge);
        network.runNetwork();

        String externalPlegeId = ((DDRObligationState) futurePledge.get().getTx().getOutput(0)).getExternalId();
        bc.startFlow(new ApproveDDRPledge.Initiator(externalPlegeId));
        network.runNetwork();

    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        Party receiver = b.getInfo().getLegalIdentities().get(0);
        AtomicExchangeDDR.Initiator flow = new AtomicExchangeDDR.Initiator("senderRIB", "receiverRIB",receiver,
                amount1000, new Date());
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        for (StartedMockNode node : ImmutableList.of(a, b, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }


}
