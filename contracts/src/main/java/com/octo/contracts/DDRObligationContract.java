package com.octo.contracts;

import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class DDRObligationContract implements Contract {

    public static final String ID = "com.octo.contracts.DDRObligationContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        requireThat(require -> {
            Stream<DDRObligationState> allStates =
                    Stream.concat(tx.inputsOfType(DDRObligationState.class).stream(), tx.outputsOfType(DDRObligationState.class).stream());
            require.using("Amount must be positive.", allStates.allMatch(obli -> obli.getAmount().getQuantity() > 0));
            return null;
        });

        if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.RequestDDRPledge)
            verifyRequestPledge(tx);
        else if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.ApproveDDRPledge)
            verifyApprovePledge(tx);
        else if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.DenyDDRPledge ||
                tx.getCommand(0).getValue() instanceof DDRObligationCommands.CancelDDRPledge)
            verifyDenyOrCancelPledge(tx);

        else if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.RequestDDRRedeem)
            verifyRequestRedeem(tx);
        else if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.ApproveDDRRedeem)
            verifyApproveRedeem(tx);
        else if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.DenyDDRRedeem)
            verifyDenyRedeem(tx);
        else if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.CancelDDRRedeem)
            verifyCancelRedeem(tx);
    }

    private void verifyRequestPledge(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("Only 1 output DDRObligationState should be created when requesting DDR Pledge",
                    tx.getOutputs().size() == 1 && tx.outputsOfType(DDRObligationState.class).size() == 1);
            require.using("No inputs should be consumed when requesting DDR Pledge", tx.getInputs().isEmpty());
            DDRObligationState output = tx.outputsOfType(DDRObligationState.class).get(0);
            require.using("Output DDRObligationState should have type Pledge when requesting DDR Pledge",
                    output.getType() == DDRObligationType.PLEDGE);
            require.using("Output DDRObligationState should have status REQUEST when requesting DDR Pledge",
                    output.getStatus() == DDRObligationStatus.REQUEST);
            return null;
        });
    }

    private void verifyApprovePledge(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("1 output of type DDRObligationState must be created when approving DDR Pledge",
                    tx.outputsOfType(DDRObligationState.class).size() == 1);
            require.using("1 input of type DDRObligationState should be consumed when approving DDR Pledge",
                    tx.getInputs().size() == 1 && tx.inputsOfType(DDRObligationState.class).size() == 1);
            DDRObligationState output = tx.outputsOfType(DDRObligationState.class).get(0);
            DDRObligationState input = tx.inputsOfType(DDRObligationState.class).get(0);
            require.using("Input DDRObligationState should have type PLEDGE", input.getType() == DDRObligationType.PLEDGE);
            require.using("Input DDRObligationState should have status REQUEST", input.getStatus() == DDRObligationStatus.REQUEST);
            require.using("Output DDRObligationState should have status APPROVED", output.getStatus() == DDRObligationStatus.APPROVED);
            require.using("Input and output DDRObligationState should have same ExternalId",
                    input.getExternalId().equals(output.getExternalId()));
            require.using("Input and output DDRObligationState should have same attributes except Status",
                    compareStatesAttributesExceptStatus(input, output));
            return null;
        });
    }

    private boolean compareStatesAttributesExceptStatus(DDRObligationState st1, DDRObligationState st2) {
        return st1.getRequesterDate().compareTo(st2.getRequesterDate()) == 0 && st1.getRequester().equals(st2.getRequester())
                && st1.getOwner().equals(st2.getOwner()) && st1.getAmount().equals(st2.getAmount()) &&
                st1.getIssuer().equals(st2.getIssuer()) && st1.getType().equals(st2.getType());
    }

    private void verifyDenyOrCancelPledge(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("No outputs must be created when denying or canceling DDR Pledge", tx.getOutputs().isEmpty());
            require.using("Only 1 input of type DDRObligationState should be consumed when denying or canceling DDR Pledge",
                    tx.getInputs().size() == 1 && tx.inputsOfType(DDRObligationState.class).size() == 1);
            DDRObligationState input = tx.inputsOfType(DDRObligationState.class).get(0);
            require.using("Input DDRObligationState should have type PLEDGE", input.getType() == DDRObligationType.PLEDGE);
            require.using("Input DDRObligationState should have status REQUEST", input.getStatus() == DDRObligationStatus.REQUEST);
            return null;
        });
    }

    private void verifyRequestRedeem(LedgerTransaction tx) {
        requireThat(require -> {
            return null;
        });
    }

    private void verifyApproveRedeem(LedgerTransaction tx) {
        requireThat(require -> {

            return null;
        });
    }

    private void verifyDenyRedeem(LedgerTransaction tx) {
        requireThat(require -> {

            return null;
        });
    }

    private void verifyCancelRedeem(LedgerTransaction tx) {
        requireThat(require -> {

            return null;
        });
    }


    public interface DDRObligationCommands extends CommandData {
        class RequestDDRPledge implements DDRObligationCommands {
        }

        class CancelDDRPledge implements DDRObligationCommands {
        }

        class ApproveDDRPledge implements DDRObligationCommands {
        }

        class DenyDDRPledge implements DDRObligationCommands {
        }

        class RequestDDRRedeem implements DDRObligationCommands {
        }

        class CancelDDRRedeem implements DDRObligationCommands {
        }

        class ApproveDDRRedeem implements DDRObligationCommands {
        }

        class DenyDDRRedeem implements DDRObligationCommands {
        }
    }
}
