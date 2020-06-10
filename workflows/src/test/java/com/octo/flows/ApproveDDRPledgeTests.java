package com.octo.flows;

import com.google.common.collect.ImmutableList;
import com.octo.enums.DDRObligationStatus;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObligationState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
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
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ApproveDDRPledgeTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    private final StartedMockNode a = network.createNode(CordaX500Name.parse("O=BankA,L=New York,C=US"));
    private final StartedMockNode bc = network.createNode(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
    private String externalId;
    private final Amount<Currency> amount = new Amount<Currency>(1000, Currency.getInstance("MAD"));

    public ApproveDDRPledgeTests() {
        a.registerInitiatedFlow(ApproveDDRPledge.Responder.class);
        bc.registerInitiatedFlow(RequestDDRPledge.Responder.class);
    }

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        network.runNetwork();
        RequestDDRPledge.Initiator flow = new RequestDDRPledge.Initiator(amount);

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
        ApproveDDRPledge.Initiator flow = new ApproveDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        ApproveDDRPledge.Initiator flow = new ApproveDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        ApproveDDRPledge.Initiator flow = new ApproveDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(bc.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void recordedTransactionHasSingleInputAndMoreThanTwoOutputs() throws Exception {
        ApproveDDRPledge.Initiator flow = new ApproveDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            LedgerTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId()).toLedgerTransaction(node.getServices());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getOutputs();
            List<ContractState> txInputs = recordedTx.getInputStates();
            assertThat(txOutputs.size(), greaterThan(1));
            assertEquals(1, txInputs.size());

            DDRObligationState recordedState = recordedTx.outputsOfType(DDRObligationState.class).get(0);
            DDRObligationState consumedState = (DDRObligationState) txInputs.get(0);

            assertEquals(consumedState.getAmount(), recordedState.getAmount());
            assertEquals(consumedState.getOwner(), recordedState.getOwner());
            assertEquals(consumedState.getRequester(), recordedState.getRequester());
            assertEquals(consumedState.getIssuer(), recordedState.getIssuer());
            assertEquals(consumedState.getRequesterDate(), recordedState.getRequesterDate());
            assertEquals(consumedState.getType(), recordedState.getType());
            assertEquals(DDRObligationStatus.APPROVED, recordedState.getStatus());
        }
    }

    @Test
    public void flowRecordsTheCorrectDDRObligationInBothPartiesVaults() throws Exception {
        ApproveDDRPledge.Initiator flow = new ApproveDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded DDR obligation in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            node.transaction(() -> {
                List<StateAndRef<DDRObligationState>> ddrObligations = node.getServices().getVaultService().queryBy(DDRObligationState.class).getStates();
                assertEquals(1, ddrObligations.size());
                DDRObligationState recordedState = ddrObligations.get(0).getState().getData();
                assertEquals(DDRObligationStatus.APPROVED, recordedState.getStatus());
                assertEquals(externalId, recordedState.getExternalId());
                return null;
            });
        }
    }

    @Test
    public void recordedTransactionHasDDRObjectsOutputTotalAMountEqualToPledgeAmount() throws Exception {
        ApproveDDRPledge.Initiator flow = new ApproveDDRPledge.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            LedgerTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId()).toLedgerTransaction(node.getServices());

            DDRObligationState consumedState = recordedTx.inputsOfType(DDRObligationState.class).get(0);
            List<DDRObjectState> recordedObjects = recordedTx.outputsOfType(DDRObjectState.class);

            assertEquals(consumedState.getAmount(), recordedObjects.stream().map(DDRObjectState::getAmount).reduce(Amount::plus).get());
            recordedObjects.forEach(object -> {
                assertEquals(consumedState.getIssuer(), object.getIssuer());
                assertEquals(consumedState.getOwner(), object.getOwner());
                assertEquals(consumedState.getCurrency(), object.getCurrency());
            });
        }
    }

}