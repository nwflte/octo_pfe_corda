package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.DDRObligationStateBuilder;
import net.corda.core.contracts.Amount;
import org.junit.Test;

import java.util.Currency;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ApproveRedeemContractTests extends BaseObligationContractTests {

    @Test
    public void approveRedeemShouldHaveOneOutputRedeemApproved() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRRedeem());

                tx.input(DDRObligationContract.ID, exampleRedeemRequest);
                tx.input(DDRObjectContract.ID, exampleDDRObject);
                tx.failsWith("1 output of type DDRObligationState must be created when approving DDR Redeem");

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, exampleRedeemRequest);
                    return tw.failsWith("Output DDRObligationState should have status APPROVED");
                });

                tx.output(DDRObligationContract.ID, exampleRedeemApproved);
                tx.verifies();
                tx.output(DDRObligationContract.ID, exampleRedeemApproved);
                return tx.failsWith("1 output of type DDRObligationState must be created when approving DDR Redeem");

            });
            return null;
        }));
    }

    @Test
    public void approveRedeemShouldHaveInputTotalAmountOfDDREqualAmountOfRedeemRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRRedeem());

                tx.input(DDRObligationContract.ID, exampleRedeemRequest);

                tx.output(DDRObligationContract.ID, exampleRedeemApproved);

                tx.tweak(tw -> {
                    tw.input(DDRObjectContract.ID, new DDRObjectStateBuilder(exampleDDRObject)
                            .amount(new Amount<Currency>(999, Currency.getInstance("MAD"))).build());
                    return tw.failsWith("Redeemed amount should be equal to total amount of consumed DDR Objects");
                });

                tx.input(DDRObjectContract.ID, exampleDDRObject);
                return tx.verifies();
            });
            return null;
        }));
    }


    @Test
    public void approveRedeemShouldHaveOneInputRedeemRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRRedeem());

                tx.output(DDRObligationContract.ID, exampleRedeemApproved);

                tx.input(DDRObjectContract.ID, exampleDDRObject);

                tx.failsWith("1 input of type DDRObligationState should be consumed when approving DDR Redeem");

                tx.tweak(tw -> {
                    tw.input(DDRObligationContract.ID, examplePledgeRequest);
                    return tw.failsWith("Input DDRObligationState should have type REDEEM");
                });

                tx.tweak(tw -> {
                    tw.input(DDRObligationContract.ID, exampleRedeemApproved);
                    return tw.failsWith("Input DDRObligationState should have status REQUEST");
                });

                tx.input(DDRObligationContract.ID, exampleRedeemRequest);

                return tx.verifies();
            });
            return null;
        }));

    }

    @Test
    public void approveRedeemShouldHaveOutputAndInputRedeemWithSameExternalId() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRRedeem());


                tx.input(DDRObligationContract.ID, exampleRedeemRequest);
                tx.input(DDRObjectContract.ID, exampleDDRObject);
                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(exampleRedeemApproved).externalId("differentExternalId").build());
                    return tw.failsWith("Input and output DDRObligationState should have same ExternalId");
                });

                tx.output(DDRObligationContract.ID, exampleRedeemApproved);
                return tx.verifies();
            });
            return null;
        }));
    }

    @Test
    public void approvePledgeShouldHaveOutputAndInputRedeemWithSameAttributes_ExceptStatus() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRRedeem());

                tx.input(DDRObligationContract.ID, exampleRedeemRequest);
                tx.input(DDRObjectContract.ID, exampleDDRObject);
                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(exampleRedeemApproved).requester(bankB.getParty()).build());
                    return tw.failsWith("Input and output DDRObligationState should have same attributes except Status");
                });

                tx.output(DDRObligationContract.ID, exampleRedeemApproved);
                return tx.verifies();
            });
            return null;
        }));
    }

}
