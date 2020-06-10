package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.builders.DDRObjectStateBuilder;
import com.octo.builders.InterBankTransferStateBuilder;
import com.octo.states.DDRObjectState;
import com.octo.states.InterBankTransferState;
import net.corda.core.contracts.Amount;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class InterBankTransferContractTest extends BaseObligationContractTests {

    private final DDRObjectState ddr1000 = exampleDDRObject;
    private final DDRObjectStateBuilder ddrBuilder = new DDRObjectStateBuilder(ddr1000);
    private final DDRObjectState ddr500 = ddrBuilder.amount(500).build();
    private final DDRObjectState ddr300 = ddrBuilder.amount(300).build();
    private final DDRObjectState ddr200 = ddrBuilder.amount(200).build();

    private final InterBankTransferState interTransfer1000 = new InterBankTransferState("senderRIB", "ReceiverRIB",
            bankA.getParty(), bankB.getParty(), new Amount<>(exampleAmount, exampleCurrency), exampleDate, "externalId");

    @Test
    public void transferShouldHaveOneOutputAndNoInput_InterBankState() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new InterBankTransferContract.InterBankTransferCommands.BankTransfer());

                tx.input(DDRObjectContract.ID, ddr1000);
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr500).owner(bankB.getParty()).build());
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr300).owner(bankB.getParty()).build());
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr200).owner(bankB.getParty()).build());

                tx.failsWith("Exactly 1 InterBank Transfer State should be created in a transfer");

                tx.output(InterBankTransferContract.ID, interTransfer1000);
                tx.verifies();

                tx.tweak(tw -> {
                    tw.input(InterBankTransferContract.ID, interTransfer1000);
                    return tw.failsWith("No InterBank Transfer State should be consumed in a transfer");
                });

                tx.output(InterBankTransferContract.ID, interTransfer1000);
                return tx.failsWith("Exactly 1 InterBank Transfer State should be created in a transfer");

            });
            return null;
        }));
    }

    @Test
    public void transferShouldVerifyAndPreserveOwnership() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new InterBankTransferContract.InterBankTransferCommands.BankTransfer());

                tx.output(InterBankTransferContract.ID, interTransfer1000);
                tx.input(DDRObjectContract.ID, ddr500);
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr500).owner(bankB.getParty()).build());
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr300).owner(bankB.getParty()).build());
                tx.failsWith("Sender Bank should consume sufficient DDR Objects in an interbank transfer");

                tx.input(DDRObjectContract.ID, ddr500);
                tx.failsWith("Receiver Bank should own output DDR Objects equal to transfer amount in an interbank transfer");

                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr200).owner(bankB.getParty()).build());
                tx.verifies();

                tx.tweak(tw -> {
                    tw.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr200).owner(bankB.getParty()).build());
                    return tw.failsWith("Receiver Bank should own output DDR Objects equal to transfer amount in an interbank transfer");
                });

                tx.tweak(tw -> {
                    tw.input(InterBankTransferContract.ID, ddr300);
                    return tw.failsWith("Sender Bank should own output DDR Objects equal to rest in an interbank transfer");
                });

                tx.output(InterBankTransferContract.ID, ddr300);
                return tx.failsWith("Sender Bank should own output DDR Objects equal to rest in an interbank transfer");
            });
            return null;
        }));
    }

    @Test
    public void transferShouldHaveDifferentSendersAndReceivers() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new InterBankTransferContract.InterBankTransferCommands.BankTransfer());

                tx.input(DDRObjectContract.ID, ddr1000);
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr500).owner(bankB.getParty()).build());
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr300).owner(bankB.getParty()).build());
                tx.output(DDRObjectContract.ID, new DDRObjectStateBuilder(ddr200).owner(bankB.getParty()).build());

                tx.tweak(tw -> {
                    tw.output(InterBankTransferContract.ID, new InterBankTransferStateBuilder(interTransfer1000).receiverBank(bankA.getParty()).build());
                    return tw.failsWith("Sender and receiver banks should be different in an interbank transfer");
                });

                tx.tweak(tw -> {
                    tw.output(InterBankTransferContract.ID, new InterBankTransferStateBuilder(interTransfer1000).receiverRIB("senderRIB").build());
                    return tw.failsWith("Sender and receiver accounts should be different in an interbank transfer");
                });
                return null;
            });
            return null;
        }));
    }


}
