package com.octo.flows;

import com.google.common.collect.ImmutableList;
import com.octo.states.IntraBankTransferState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
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

import static org.junit.Assert.assertEquals;

public class RecordIntraBankTransferTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    private final StartedMockNode a = network.createNode(CordaX500Name.parse("O=BankA,L=New York,C=US"));
    private final StartedMockNode bc = network.createNode(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));
    private final Amount<Currency> amount = new Amount<Currency>(1000, Currency.getInstance("MAD"));
    private final Date date = new Date();

    public RecordIntraBankTransferTests(){
        bc.registerInitiatedFlow(RecordIntraBankTransfer.Responder.class);
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
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        RecordIntraBankTransfer.Initiator flow = new RecordIntraBankTransfer.Initiator(amount, "senderRIB", "receiverRIB", date);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        RecordIntraBankTransfer.Initiator flow = new RecordIntraBankTransfer.Initiator(amount, "senderRIB", "receiverRIB", date);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(bc.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        RecordIntraBankTransfer.Initiator flow = new RecordIntraBankTransfer.Initiator(amount, "senderRIB", "receiverRIB", date);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutput() throws Exception {
        RecordIntraBankTransfer.Initiator flow = new RecordIntraBankTransfer.Initiator(amount, "senderRIB", "receiverRIB", date);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            assert recordedTx != null;
            List<IntraBankTransferState> txOutputs = recordedTx.getTx().outputsOfType(IntraBankTransferState.class);
            assertEquals(1, txOutputs.size());
            assertEquals(0, recordedTx.getTx().getInputs().size());

            IntraBankTransferState recordedState = txOutputs.get(0);
            assertEquals(amount, recordedState.getAmount());
            assertEquals(a.getInfo().getLegalIdentities().get(0), recordedState.getBank());
            assertEquals(date, recordedState.getExecutionDate());
            assertEquals("senderRIB", recordedState.getSenderRIB());
            assertEquals("receiverRIB", recordedState.getReceiverRIB());
        }
    }

    @Test
    public void flowRecordsTheCorrectDDRObligationInBothPartiesVaults() throws Exception {
        RecordIntraBankTransfer.Initiator flow = new RecordIntraBankTransfer.Initiator(amount, "senderRIB", "receiverRIB", date);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        future.get();

        // We check the recorded DDR obligation in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            node.transaction(() -> {
                List<StateAndRef<IntraBankTransferState>> recordedTransfers = node.getServices().getVaultService().queryBy(IntraBankTransferState.class).getStates();
                assertEquals(1, recordedTransfers.size());
                IntraBankTransferState recordedState = recordedTransfers.get(0).getState().getData();
                assertEquals(amount, recordedState.getAmount());
                assertEquals(a.getInfo().getLegalIdentities().get(0), recordedState.getBank());
                assertEquals(date, recordedState.getExecutionDate());
                assertEquals("senderRIB", recordedState.getSenderRIB());
                assertEquals("receiverRIB", recordedState.getReceiverRIB());
                return null;
            });
        }
    }



}
