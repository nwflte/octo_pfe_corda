package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.DDRObligationStateBuilder;
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract;
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand;
import org.junit.Test;

import java.util.Collections;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ApprovePledgeContractTests extends BaseObligationContractTests {


    @Test
    public void approvePledgeShouldHaveOneOutputPledgeApproved() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.ApproveDDRPledge());
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new IssueTokenCommand(exampleDDRObject.getIssuedTokenType(), Collections.singletonList(0)));

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(FungibleTokenContract.Companion.getContractId(), exampleDDRObject);
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
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new IssueTokenCommand(exampleDDRObject.getIssuedTokenType(), Collections.singletonList(1)));

                tx.input(DDRObligationContract.ID, examplePledgeRequest);

                tx.output(DDRObligationContract.ID, examplePledgeApproved);

                tx.tweak(tw -> {
                    tw.output(FungibleTokenContract.Companion.getContractId(), new DDRObjectStateBuilder(exampleDDRObject).amount(999).build());
                    return tw.failsWith("Pledged amount should be equal to total amount of issued DDR Objects");
                });

                tx.output(FungibleTokenContract.Companion.getContractId(), exampleDDRObject);
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
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new IssueTokenCommand(exampleDDRObject.getIssuedTokenType(), Collections.singletonList(1)));

                tx.output(DDRObligationContract.ID, examplePledgeApproved);

                tx.output(FungibleTokenContract.Companion.getContractId(), exampleDDRObject);

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
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new IssueTokenCommand(exampleDDRObject.getIssuedTokenType(), Collections.singletonList(0)));


                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(FungibleTokenContract.Companion.getContractId(), exampleDDRObject);
                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeApproved).externalId("differentExternalId").build());
                    return tw.failsWith("Input and output DDRObligationState should have same ExternalId");
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
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new IssueTokenCommand(exampleDDRObject.getIssuedTokenType(), Collections.singletonList(0)));

                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                tx.output(FungibleTokenContract.Companion.getContractId(), exampleDDRObject);
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
