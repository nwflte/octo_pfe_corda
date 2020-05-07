package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.DDRObligationStateBuilder;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ApprovePledgeContractTests extends BaseObligationContractTests {

    @Test
    public void approvePledgeShouldHaveOneOutputPledgeApproved() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(DDRObjectContract.ID, exampleDDRObject);
                tx.failsWith("1 output of type DDRObligationState must be created when approving DDR Pledge");

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, examplePledgeRequest);
                    return tw.failsWith("Output DDRObligationState should have status APPROVED");
                });

                tx.output(DDRObligationContract.ID, examplePledgeApproved);
                tx.verifies();
                tx.output(DDRObligationContract.ID, examplePledgeApproved);
                return tx.failsWith("1 output of type DDRObligationState must be created when approving DDR Pledge");

            });
            return null;
        }));
    }

    @Test
    public void approvePledgeShouldHaveOutputTotalAmountOfDDREqualAmountOfPledgeRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.input(DDRObligationContract.ID, examplePledgeRequest);

                tx.output(DDRObligationContract.ID, examplePledgeApproved);

                tx.tweak(tw -> {
                    tw.output(DDRObjectContract.ID, new DDRObjectStateBuilder(exampleDDRObject).amount(999).build());
                    return tw.failsWith("Pledged amount should be equal to total amount of issued DDR Objects");
                });

                tx.output(DDRObjectContract.ID, exampleDDRObject);
                return tx.verifies();
            });
            return null;
        }));
    }


    @Test
    public void approvePledgeShouldHaveOneInputPledgeRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.output(DDRObligationContract.ID, examplePledgeApproved);

                tx.output(DDRObjectContract.ID, exampleDDRObject);

                tx.failsWith("1 input of type DDRObligationState should be consumed when approving DDR Pledge");

                tx.tweak(tw -> {
                    tw.input(DDRObligationContract.ID, exampleRedeemRequest);
                    return tw.failsWith("Input DDRObligationState should have type PLEDGE");
                });

                tx.tweak(tw -> {
                    tw.input(DDRObligationContract.ID, examplePledgeApproved);
                    return tw.failsWith("Input DDRObligationState should have status REQUEST");
                });

                tx.input(DDRObligationContract.ID, examplePledgeRequest);

                return tx.verifies();
            });
            return null;
        }));

    }

    @Test
    public void approvePledgeShouldHaveOutputAndInputPledgeWithSameExternalId() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());


                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(DDRObjectContract.ID, exampleDDRObject);
                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeApproved).externalId("differentExternalId").build());
                    return tw.failsWith("Input and output DDRObligationState should have same attributes except Status");
                });

                tx.output(DDRObligationContract.ID, examplePledgeApproved);
                return tx.verifies();
            });
            return null;
        }));
    }

    @Test
    public void approvePledgeShouldHaveOutputAndInputPledgeWithSameAttributes_ExceptStatus() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(DDRObjectContract.ID, exampleDDRObject);
                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeApproved).requester(bankB.getParty()).build());
                    return tw.failsWith("Input and output DDRObligationState should have same attributes except Status");
                });

                tx.output(DDRObligationContract.ID, examplePledgeApproved);
                return tx.verifies();
            });
            return null;
        }));
    }

}
