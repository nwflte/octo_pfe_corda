package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObligationStateBuilder;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class RequestDDRObligationPledgeContractTests extends BaseDDRObligationPledgeContractTests {

    @Test
    public void requestDDRPledgeShouldHaveOneOutput() {
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
    public void requestDDRPledgeShouldHaveNoInputs() {
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
    public void requestDDRPledgeShouldHaveOneOutputWithType_Pledge_AndStatus_Request() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()),
                        new DDRObligationContract.DDRObligationCommands.RequestDDRPledge());

                tx.tweak(tw -> {

                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).type(DDRObligationType.REDEEM).build());
                    return tw.failsWith("Output DDRObligationState should have type Pledge when requesting DDR Pledge");
                });

                tx.tweak(tw -> {
                    tw.output(DDRObligationContract.ID, new DDRObligationStateBuilder(examplePledgeRequest).status(DDRObligationStatus.APPROVED).build());
                    return tw.failsWith("Output DDRObligationState should have status REQUEST when requesting DDR Pledge");
                });

                tx.output(DDRObligationContract.ID, examplePledgeRequest);
                return tx.verifies();
            });
            return null;
        }));
    }

}