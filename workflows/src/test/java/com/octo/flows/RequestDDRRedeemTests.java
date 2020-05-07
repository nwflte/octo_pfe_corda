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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class RequestDDRRedeemTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    private final StartedMockNode a = network.createNode(CordaX500Name.parse("O=BankA,L=New York,C=US"));
    private final StartedMockNode bc = network.createNode(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));


    public RequestDDRRedeemTests() {
        bc.registerInitiatedFlow(RequestDDRRedeem.Responder.class);
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws ExecutionException, InterruptedException {
        RequestDDRRedeem.Initiator flow = new RequestDDRRedeem.Initiator(
                new Amount<>(1000, Currency.getInstance("MAD")));

        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void negativeAmount_ThrowsException() throws Exception {
        exception.expect(instanceOf(IllegalArgumentException.class));
        RequestDDRRedeem.Initiator flow = new RequestDDRRedeem.Initiator(
                new Amount<>(-1, Currency.getInstance("MAD")));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        RequestDDRRedeem.Initiator flow = new RequestDDRRedeem.Initiator(
                new Amount<>(1, Currency.getInstance("MAD")));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(bc.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        RequestDDRRedeem.Initiator flow = new RequestDDRRedeem.Initiator(
                new Amount<>(1, Currency.getInstance("MAD")));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutput() throws Exception {
        Amount<Currency> amount = new Amount<>(1, Currency.getInstance("MAD"));
        RequestDDRRedeem.Initiator flow = new RequestDDRRedeem.Initiator(amount);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assertEquals(1, txOutputs.size());
            assertEquals(0, recordedTx.getTx().getInputs().size());

            DDRObligationState recordedState = (DDRObligationState) txOutputs.get(0).getData();
            assertEquals(amount, recordedState.getAmount());
            assertEquals(a.getInfo().getLegalIdentities().get(0), recordedState.getOwner());
            assertEquals(a.getInfo().getLegalIdentities().get(0), recordedState.getRequester());
            assertEquals(bc.getInfo().getLegalIdentities().get(0), recordedState.getIssuer());
            assertEquals(DDRObligationType.REDEEM, recordedState.getType());
            assertEquals(DDRObligationStatus.REQUEST, recordedState.getStatus());
        }
    }

    @Test
    public void flowRecordsTheCorrectDDRObligationInBothPartiesVaults() throws Exception {
        Amount<Currency> amount = new Amount<>(1, Currency.getInstance("MAD"));
        RequestDDRRedeem.Initiator flow = new RequestDDRRedeem.Initiator(amount);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        future.get();

        // We check the recorded DDR obligation in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            node.transaction(() -> {
                List<StateAndRef<DDRObligationState>> ddrObligations = node.getServices().getVaultService().queryBy(DDRObligationState.class).getStates();
                assertEquals(1, ddrObligations.size());
                DDRObligationState recordedState = ddrObligations.get(0).getState().getData();
                assertEquals(amount, recordedState.getAmount());
                assertEquals(a.getInfo().getLegalIdentities().get(0), recordedState.getOwner());
                assertEquals(a.getInfo().getLegalIdentities().get(0), recordedState.getRequester());
                assertEquals(DDRObligationType.REDEEM, recordedState.getType());
                assertEquals(DDRObligationStatus.REQUEST, recordedState.getStatus());
                return null;
            });
        }
    }
}