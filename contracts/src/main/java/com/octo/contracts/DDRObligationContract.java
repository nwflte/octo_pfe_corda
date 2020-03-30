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
            require.using("", allStates.allMatch(obli -> obli.getAmount().getQuantity() > 0));
            return null;
        });

        if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.RequestDDRPledge)
            verifyRequest(tx, DDRObligationType.PLEDGE);
        else if (tx.getCommand(0).getValue() instanceof DDRObligationCommands.ApproveDDRPledge)
            verifyApprovePledge(tx);
    }

    private void verifyRequest(LedgerTransaction tx, DDRObligationType type) {
        requireThat(require -> {
            require.using("", tx.getOutputs().size() == 1 && tx.outputsOfType(DDRObligationState.class).size() == 1);
            require.using("", tx.getInputs().isEmpty());
            DDRObligationState output = tx.outputsOfType(DDRObligationState.class).get(0);
            require.using("", output.getType() == type && output.getStatus() == DDRObligationStatus.REQUEST);
            return null;
        });
    }

    private void verifyApprovePledge(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("", tx.outputsOfType(DDRObligationState.class).size() == 1);
            require.using("", tx.getInputs().size() == 1 && tx.inputsOfType(DDRObligationState.class).size() == 1);
            DDRObligationState output = tx.outputsOfType(DDRObligationState.class).get(0);
            DDRObligationState input = tx.inputsOfType(DDRObligationState.class).get(0);
            require.using("", input.getExternalId().equals(output.getExternalId()));
            require.using("", input.getType() == DDRObligationType.PLEDGE && input.getStatus() == DDRObligationStatus.REQUEST);
            require.using("", output.getStatus() == DDRObligationStatus.APPROVED);
            // Verify that other properties didn't change
            return null;
        });
    }

    private void verifyDenyPledge(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("", tx.outputsOfType(DDRObligationState.class).size() == 1);
            require.using("", tx.getInputs().size() == 1 && tx.inputsOfType(DDRObligationState.class).size() == 1);
            DDRObligationState output = tx.outputsOfType(DDRObligationState.class).get(0);
            DDRObligationState input = tx.inputsOfType(DDRObligationState.class).get(0);
            require.using("", input.getLinearId().compareTo(output.getLinearId()) == 0);
            require.using("", input.getType() == DDRObligationType.PLEDGE && input.getStatus() == DDRObligationStatus.REQUEST);
            require.using("", output.getStatus() == DDRObligationStatus.APPROVED);
            return null;
        });
    }

    private void verifyCancelPledge(LedgerTransaction tx) {
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
