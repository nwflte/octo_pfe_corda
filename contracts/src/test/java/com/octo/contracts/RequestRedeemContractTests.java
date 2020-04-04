package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class RequestRedeemContractTests extends BaseObligationContractTests {

    @Test
    public void requestRedeemShouldHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.RequestDDRRedeem());
                tx.output(DDRObligationContract.ID, exampleRedeemRequest);
                tx.verifies();
                tx.output(DDRObligationContract.ID, exampleRedeemRequest);
                return tx.failsWith("Only 1 output DDRObligationState should be created when requesting DDR Redeem");
            });
            return null;
        }));
    }

    @Test
    public void requestRedeemShouldHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.RequestDDRRedeem());
                tx.output(DDRObligationContract.ID, exampleRedeemRequest);
                tx.verifies();
                tx.input(DDRObligationContract.ID, exampleRedeemRequest);
                return tx.failsWith("No inputs should be consumed when requesting DDR Redeem");
            });
            return null;
        }));
    }

    @Test
    public void requestRedeemShouldHaveOneOutputRedeemRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.RequestDDRRedeem());

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, examplePledgeRequest);
                    return tw.failsWith("Output DDRObligationState should have type Redeem when requesting DDR Redeem");
                });

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, exampleRedeemApproved);
                    return tw.failsWith("Output DDRObligationState should have status REQUEST when requesting DDR Redeem");
                });

                tx.output(DDRObligationContract.ID, exampleRedeemRequest);
                return tx.verifies();
            });
            return null;
        }));
    }

}