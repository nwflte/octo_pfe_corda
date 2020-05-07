package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class DenyOrCancelPledgeContractTests extends BaseObligationContractTests {

    @Test
    public void denyPledgeShouldHaveOneInputPledgeRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.DenyDDRPledge());

                tx.output(DDRObligationContract.ID, examplePledgeRejected);

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, examplePledgeApproved);
                    return tw2.failsWith("Input DDRObligationState should have status REQUEST");
                });

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, exampleRedeemRequest);
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
    public void cancelPledgeShouldHaveOneInputPledgeRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.CancelDDRPledge());

                tx.output(DDRObligationContract.ID, examplePledgeCanceled);

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, examplePledgeApproved);
                    return tw2.failsWith("Input DDRObligationState should have status REQUEST");
                });

                tx.tweak(tw2 -> {
                    tw2.input(DDRObligationContract.ID, exampleRedeemRequest);
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
    public void denyPledgeShouldHaveOneOutputPledgeRejected() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.DenyDDRPledge());
                tx.failsWith("1 output of type DDRObligationState must be created when denying or canceling DDR Pledge");

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, examplePledgeCanceled);
                    return tw.failsWith("Output DDRObligationState should have status REJECTED");
                });

                tx.output(DDRObligationContract.ID, examplePledgeRejected);
                return tx.verifies();
            });
            return null;
        }));
    }

    @Test
    public void cancelPledgeShouldHaveNoOutputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.CancelDDRPledge());

                tx.failsWith("1 output of type DDRObligationState must be created when denying or canceling DDR Pledge");

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, examplePledgeRejected);
                    return tw.failsWith("Output DDRObligationState should have status CANCELED");
                });

                tx.output(DDRObligationContract.ID, examplePledgeCanceled);
                return tx.verifies();
            });
            return null;
        }));
    }

}
