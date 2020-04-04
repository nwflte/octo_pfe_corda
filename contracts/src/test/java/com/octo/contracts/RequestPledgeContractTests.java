package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class RequestPledgeContractTests extends BaseObligationContractTests {

    @Test
    public void requestPledgeShouldHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.RequestDDRPledge());
                tx.output(DDRObligationContract.ID, examplePledgeRequest);
                tx.verifies();
                tx.output(DDRObligationContract.ID, examplePledgeRequest);
                return tx.failsWith("Only 1 output DDRObligationState should be created when requesting DDR Pledge");
            });
            return null;
        }));
    }

    @Test
    public void requestPledgeShouldHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.RequestDDRPledge());
                tx.output(DDRObligationContract.ID, examplePledgeRequest);
                tx.verifies();
                tx.input(DDRObligationContract.ID, examplePledgeRequest);
                return tx.failsWith("No inputs should be consumed when requesting DDR Pledge");
            });
            return null;
        }));
    }

    @Test
    public void requestPledgeShouldHaveOneOutputPledgeRequest() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.RequestDDRPledge());

                tx.tweak(tw -> {

                    tw.output(DDRObligationContract.ID, exampleRedeemRequest);
                    return tw.failsWith("Output DDRObligationState should have type Pledge when requesting DDR Pledge");
                });

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, examplePledgeApproved);
                    return tw.failsWith("Output DDRObligationState should have status REQUEST when requesting DDR Pledge");
                });

                tx.output(DDRObligationContract.ID, examplePledgeRequest);
                return tx.verifies();
            });
            return null;
        }));
    }

}