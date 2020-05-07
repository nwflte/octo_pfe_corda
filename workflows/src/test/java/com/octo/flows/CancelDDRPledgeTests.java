package com.octo.flows;

import com.google.common.collect.ImmutableList;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObligationState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class CancelDDRPledgeTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    private final StartedMockNode a = network.createNode(CordaX500Name.parse("O=BankA,L=New York,C=US"));
    private final StartedMockNode bc = network.createNode(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
    private String externalId;


    public CancelDDRPledgeTests() {
        bc.registerInitiatedFlow(CancelDDRPledge.Responder.class);
        bc.registerInitiatedFlow(RequestDDRPledge.Responder.class);
    }

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        network.runNetwork();
        RequestDDRPledge.Initiator flow = new RequestDDRPledge.Initiator(
                new Amount<Currency>(1000, Currency.getInstance("MAD")));

        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        externalId = ((DDRObligationState) future.get().getTx().getOutput(0)).getExternalId();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        CancelDDRPledge.Initiator flow = new CancelDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        CancelDDRPledge.Initiator flow = new CancelDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(bc.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        CancelDDRPledge.Initiator flow = new CancelDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void recordedTransactionHasSingleInputAndNoOutput() throws Exception {
        CancelDDRPledge.Initiator flow = new CancelDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            LedgerTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId()).toLedgerTransaction(node.getServices());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getOutputs();
            List<ContractState> txInputs = recordedTx.getInputStates();
            assertEquals(1, txOutputs.size());
            assertEquals(1, txInputs.size());

            DDRObligationState consumedState = (DDRObligationState) txInputs.get(0);

            assertEquals(DDRObligationType.PLEDGE, consumedState.getType());
            assertEquals(DDRObligationStatus.REQUEST, consumedState.getStatus());
        }
    }

    @Test
    public void flowRecordsTheCorrectDDRObligationInBothPartiesVaults() throws Exception {
        CancelDDRPledge.Initiator flow = new CancelDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the DDR obligation in both vaults is consumed.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            node.transaction(() -> {
                QueryCriteria queryCriteria = new QueryCriteria
                        .LinearStateQueryCriteria(null, null, ImmutableList.of(externalId), Vault.StateStatus.CONSUMED);
                List<StateAndRef<DDRObligationState>> ddrObligations = node.getServices().getVaultService()
                        .queryBy(DDRObligationState.class, queryCriteria).getStates();
                assertEquals(1, ddrObligations.size());
                DDRObligationState recordedState = ddrObligations.get(0).getState().getData();
                assertEquals(DDRObligationStatus.REQUEST, recordedState.getStatus());
                return null;
            });
        }
    }

}
