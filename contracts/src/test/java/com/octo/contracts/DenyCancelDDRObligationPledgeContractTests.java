package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObligationStateBuilder;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class DenyCancelDDRObligationPledgeContractTests extends BaseDDRObligationPledgeContractTests {

    @Test
    public void denyDDRPledgeShouldHaveOneInputDDRObligationOfType_Pledge_andStatus_Request() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.DenyDDRPledge());

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                    return tw2.failsWith("Input DDRObligationState should have status REQUEST");
                });

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).type(DDRObligationType.REDEEM).build());
                    return tw2.failsWith("Input DDRObligationState should have type PLEDGE");
                });

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.verifies();

                tx.input(DDRObjectContract.ID, exampleDDRObject);

                return tx.failsWith("Only 1 input of type DDRObligationState should be consumed when denying or canceling DDR Pledge");
            });
            return null;
        }));
    }

    @Test
    public void cancelDDRPledgeShouldHaveOneInputDDRObligationOfType_Pledge_andStatus_Request() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.CancelDDRPledge());

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                    return tw2.failsWith("Input DDRObligationState should have status REQUEST");
                });

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).type(DDRObligationType.REDEEM).build());
                    return tw2.failsWith("Input DDRObligationState should have type PLEDGE");
                });

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.verifies();

                tx.input(DDRObjectContract.ID, exampleDDRObject);

                return tx.failsWith("Only 1 input of type DDRObligationState should be consumed when denying or canceling DDR Pledge");
            });
            return null;
        }));
    }

    @Test
    public void denyDDRPledgeShouldHaveNoOutputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.DenyDDRPledge());
                tx.verifies();
                tx.output(DDRObligationContract.ID, examplePledgeRequest);
                return tx.failsWith("No outputs must be created when denying or canceling DDR Pledge");

            });
            return null;
        }));
    }

    @Test
    public void cancelDDRPledgeShouldHaveNoOutputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.CancelDDRPledge());
                tx.verifies();
                tx.output(DDRObligationContract.ID, examplePledgeRequest);
                return tx.failsWith("No outputs must be created when denying or canceling DDR Pledge");

            });
            return null;
        }));
    }

}
