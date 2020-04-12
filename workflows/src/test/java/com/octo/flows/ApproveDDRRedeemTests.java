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

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ApproveDDRRedeemTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    private final StartedMockNode a = network.createNode(CordaX500Name.parse("O=BankA,L=New York,C=US"));
    private final StartedMockNode bc = network.createNode(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
    private String externalId;
    private Amount<Currency> amount3200 =  new Amount<Currency>(1500, Currency.getInstance("MAD"));
    private Amount<Currency> amount1000 =  new Amount<Currency>(500, Currency.getInstance("MAD"));

    public ApproveDDRRedeemTests() {
        a.registerInitiatedFlow(ApproveDDRRedeem.Responder.class);
        a.registerInitiatedFlow(ApproveDDRPledge.Responder.class);
        bc.registerInitiatedFlow(RequestDDRRedeem.Responder.class);
        bc.registerInitiatedFlow(RequestDDRPledge.Responder.class);
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

        RequestDDRRedeem.Initiator flowRedeem = new RequestDDRRedeem.Initiator(amount1000, new Date(new Date().getTime() - 86400000));
        CordaFuture<SignedTransaction> futureRedeem = a.startFlow(flowRedeem);
        network.runNetwork();
        externalId = ((DDRObligationState) futureRedeem.get().getTx().getOutput(0)).getExternalId();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        ApproveDDRRedeem.Initiator flow = new ApproveDDRRedeem.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        ApproveDDRRedeem.Initiator flow = new ApproveDDRRedeem.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        ApproveDDRRedeem.Initiator flow = new ApproveDDRRedeem.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(bc.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void recordedTransactionHasSingleInputAndMoreThanTwoOutputs() throws Exception {
        ApproveDDRRedeem.Initiator flow = new ApproveDDRRedeem.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            LedgerTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId()).toLedgerTransaction(node.getServices());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getOutputs();
            List<ContractState> txInputs = recordedTx.getInputStates();
            assertThat(txInputs.size(), greaterThan(1));
            assertEquals(1, txOutputs.size());

            DDRObligationState recordedState = recordedTx.outputsOfType(DDRObligationState.class).get(0);
            DDRObligationState consumedState = (DDRObligationState) txInputs.get(0);

            assertEquals(consumedState.getAmount(), recordedState.getAmount());
            assertEquals(recordedState.getOwner(), recordedState.getOwner());
            assertEquals(recordedState.getRequester(), recordedState.getRequester());
            assertEquals(recordedState.getIssuer(), recordedState.getIssuer());
            assertEquals(recordedState.getRequesterDate(), recordedState.getRequesterDate());
            assertEquals(recordedState.getType(), recordedState.getType());
            assertEquals(DDRObligationStatus.APPROVED, recordedState.getStatus());
        }
    }

    @Test
    public void flowRecordsTheCorrectDDRObligationInBothPartiesVaults() throws Exception {
        ApproveDDRRedeem.Initiator flow = new ApproveDDRRedeem.Initiator(externalId);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded DDR obligation in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            node.transaction(() -> {
                List<StateAndRef<DDRObligationState>> ddrObligations = node.getServices().getVaultService()
                        .queryBy(DDRObligationState.class).getStates();
                assertEquals(1, ddrObligations.stream().filter(obl -> obl.getState().getData().getType() == DDRObligationType.REDEEM).count());
                DDRObligationState recordedState = ddrObligations.get(0).getState().getData();
                assertEquals(DDRObligationStatus.APPROVED, recordedState.getStatus());
                return null;
            });
        }
    }

}