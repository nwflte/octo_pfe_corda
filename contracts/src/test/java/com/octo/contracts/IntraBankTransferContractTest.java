package com.octo.contracts;

import com.octo.states.IntraBankTransferState;
import com.octo.states.IntraBankTransferStateBuilder;
import net.corda.core.contracts.Amount;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class IntraBankTransferContractTest extends BaseObligationContractTests {

    IntraBankTransferState intraBankTransferState = new IntraBankTransferState("senderRIB", "ReceiverRIB", bankA.getParty(), new Amount<>(exampleAmount, exampleCurrency),
            exampleDate, "externald");

    @Test
    public void transferShouldHaveOneOutputAndNoInput_IntraBankState() {
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                tx.command(Arrays.asList(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new IntraBankTransferContract.IntraBankTransferCommands.RecordTransfer());

                tx.output(IntraBankTransferContract.ID, intraBankTransferState);

                tx.tweak(tw -> {
                    tw.output(DDRObjectContract.ID, exampleDDRObject);
                    return tw.failsWith("Recording an IntraBank Transfer should have one output that is IntraBankTransferState");
                });

                tx.tweak(tw -> {
                    tw.output(IntraBankTransferContract.ID, intraBankTransferState);
                    return tw.failsWith("Recording an IntraBank Transfer should have one output that is IntraBankTransferState");
                });

                tx.tweak(tw -> {
                    tw.input(DDRObjectContract.ID, exampleDDRObject);
                    return tw.failsWith("Recording an IntraBank Transfer should not consume any inputs");
                });
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void transferShouldHaveDifferentSenderAndReceiver() {
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                tx.command(Arrays.asList(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new IntraBankTransferContract.IntraBankTransferCommands.RecordTransfer());

                tx.tweak(tw -> {
                    IntraBankTransferState senderIsReceiverState = new IntraBankTransferStateBuilder(intraBankTransferState)
                            .receiverRIB("senderRIB").build();
                    tw.output(IntraBankTransferContract.ID, senderIsReceiverState);
                    return tw.failsWith("IntraBank Transfer should have different sender and receiver");
                });

                tx.output(IntraBankTransferContract.ID, intraBankTransferState);
                return tx.verifies();
            });
            return null;
        });
    }
}
