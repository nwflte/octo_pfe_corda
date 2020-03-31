package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.DDRObligationStateBuilder;
import net.corda.core.contracts.Amount;
import org.junit.Test;

import java.util.Currency;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ApproveDDRObligationPledgeContractTests extends BaseDDRObligationPledgeContractTests {

    @Test
    public void approveDDRPledgeShouldHaveOneOutputDDRObligation() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.failsWith("1 output of type DDRObligationState must be created when approving DDR Pledge");

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, examplePledgeRequest);
                    return tw.failsWith("Output DDRObligationState should have status APPROVED");
                });

                tx.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                tx.verifies();
                tx.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                return tx.failsWith("1 output of type DDRObligationState must be created when approving DDR Pledge");

            });
            return null;
        }));
    }

    @Test
    public void approveDDRPledgeOutputTotalAmountOfDDRObjectEqualAmountInputDDRObligation() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.input(DDRObligationContract.ID, examplePledgeRequest);

                tx.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());

                tx.tweak(tw -> {
                    tw.output(DDRObjectContract.ID, new DDRObjectStateBuilder(exampleDDRObject)
                            .amount(new Amount<Currency>(999, Currency.getInstance("MAD"))).build());
                    return tw.failsWith("Total Output Amount DDRObjectState should equal DDRObligationState amount");
                });

                tx.output(DDRObjectContract.ID, exampleDDRObject);
                return tx.verifies();
            });
            return null;
        }));
    }


    @Test
    public void approveDDRPledgeShouldHaveOneInputOfTypeDDRObjectObligationWithType_Pledge_AndStatus_Request() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());

                tx.output(DDRObjectContract.ID, exampleDDRObject);

                tx.failsWith("1 input of type DDRObligationState should be consumed when approving DDR Pledge");

                tx.tweak(tw -> {
                    tw.input(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).type(DDRObligationType.REDEEM).build());
                    return tw.failsWith("Input DDRObligationState should have type PLEDGE");
                });

                tx.tweak(tw -> {
                    tw.input(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                    return tw.failsWith("Input DDRObligationState should have status REQUEST");
                });

                tx.input(DDRObligationContract.ID, examplePledgeRequest);

                return tx.verifies();
            });
            return null;
        }));

    }

    @Test
    public void approveDDRPledgeShouldHaveOutputAndInputDDRObligationWithSameExternalId() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());


                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(DDRObjectContract.ID, exampleDDRObject);
                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest)
                            .status(DDRObligationStatus.APPROVED).externalId("differentExternalId").build());
                    return tw.failsWith("Input and output DDRObligationState should have same ExternalId");
                });

                tx.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                return tx.verifies();
            });
            return null;
        }));
    }

    @Test
    public void approveDDRPledgeShouldHaveOutputAndInputDDRObligationWithSameAttributes_ExceptStatus() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(DDRObjectContract.ID, exampleDDRObject);
                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest)
                            .status(DDRObligationStatus.APPROVED).requester(bankB.getParty()).build());
                    return tw.failsWith("Input and output DDRObligationState should have same attributes except Status");
                });

                tx.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                return tx.verifies();
            });
            return null;
        }));
    }

}
