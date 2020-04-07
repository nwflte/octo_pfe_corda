package com.octo.contracts;

import com.octo.states.DDRObjectState;
import com.octo.states.InterBankTransferState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class InterBankTransferContract implements Contract {

    public static final String ID = "com.octo.contracts.InterBankTransferContract";


    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        tx.getCommands().stream().forEach(command -> {
            CommandData commandData = command.getValue();
            if (commandData instanceof InterBankTransferCommands.BankTransfer)
                verifyBankTransfer(tx);
        });
    }

    private void verifyBankTransfer(LedgerTransaction tx) {
        List<InterBankTransferState> outputTransferStates = tx.outputsOfType(InterBankTransferState.class);
        requireThat(require -> {
            require.using("No InterBank Transfer State should be consumed in a transfer",
                    tx.inputsOfType(InterBankTransferState.class).isEmpty());
            require.using("Exactly 1 InterBank Transfer State should be created in a transfer",
                    outputTransferStates.size() == 1);
            require.using("DDR Objects should be consumed in an Atomic Exchange", !tx.inputsOfType(DDRObjectState.class).isEmpty());
            require.using("DDR Objects should be created in an Atomic Exchange", !tx.outputsOfType(DDRObjectState.class).isEmpty());
            InterBankTransferState output = outputTransferStates.get(0);
            require.using("Sender and receiver banks should be different in an interbank transfer",
                    !output.getReceiverBank().equals(output.getSenderBank()));
            require.using("Sender and receiver accounts should be different in an interbank transfer",
                    !output.getSenderRIB().equalsIgnoreCase(output.getReceiverRIB()));
            return null;
        });
        senderBankTransfersDDRAmountRequiredToReceiver(tx.inputsOfType(DDRObjectState.class), tx.outputsOfType(DDRObjectState.class), outputTransferStates.get(0));
    }

    private void senderBankTransfersDDRAmountRequiredToReceiver(List<DDRObjectState> inputs, List<DDRObjectState> outputs,
                                                                InterBankTransferState transfer) {
        long totalInputAmount = inputs.stream().mapToLong(ddr -> ddr.getAmount().getQuantity()).sum();
        long restOfTransfer = totalInputAmount - transfer.getAmount().getQuantity();

        if (restOfTransfer < 0)
            throw new IllegalArgumentException("Failed requirement: Sender Bank should consume sufficient DDR Objects in an interbank transfer");

        long totalSenderOutput = getTotalAmountOfParty(outputs, transfer.getSenderBank());
        if (totalSenderOutput != restOfTransfer)
            throw new IllegalArgumentException("Failed requirement: Sender Bank should own output DDR Objects equal to rest in an interbank transfer");


        long totalReceiverOutput = getTotalAmountOfParty(outputs, transfer.getReceiverBank());
        if (totalReceiverOutput != transfer.getAmount().getQuantity())
            throw new IllegalArgumentException("Failed requirement: Receiver Bank should own output DDR Objects equal to transfer amount in an interbank transfer");
    }

    private long getTotalAmountOfParty(List<DDRObjectState> ddrs, Party party) {
        return ddrs.stream().filter(ddr -> ddr.getOwner().equals(party)).mapToLong(ddr -> ddr.getAmount().getQuantity()).sum();
    }

    public interface InterBankTransferCommands extends CommandData {
        class BankTransfer implements InterBankTransferCommands {
        }
    }
}
