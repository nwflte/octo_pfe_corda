package com.octo.contracts;

import com.octo.states.IntraBankTransferState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IntraBankTransferContract implements Contract {

    public static final String ID = "com.octo.contracts.IntraBankTransferContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        CommandWithParties<IntraBankTransferCommands.RecordTransfer> command =
                requireSingleCommand(tx.getCommands(), IntraBankTransferCommands.RecordTransfer.class);

        requireThat(require -> {
            require.using("Recording an IntraBank Transfer should not consume any inputs", tx.getInputs().isEmpty());
            require.using("Recording an IntraBank Transfer should have one output that is IntraBankTransferState",
                    tx.getOutputs().size() == 1 && tx.outputsOfType(IntraBankTransferState.class).size() == 1);
            IntraBankTransferState transferState = tx.outputsOfType(IntraBankTransferState.class).get(0);
            require.using("IntraBank Transfer should have different sender and receiver",
                    !transferState.getReceiverRIB().equalsIgnoreCase(transferState.getSenderRIB()));
            require.using("Concerned bank should be signer of IntraBank Transfer Transaction",
                    command.getSigners().contains(transferState.getBank().getOwningKey()));
            return null;
        });
    }

    public interface IntraBankTransferCommands extends CommandData {
        class RecordTransfer implements IntraBankTransferCommands {
        }
    }
}
