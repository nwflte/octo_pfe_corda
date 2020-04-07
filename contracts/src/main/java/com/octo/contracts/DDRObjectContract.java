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

public class DDRObjectContract implements Contract {

    public static final String ID = "com.octo.contracts.DDRObjectContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx){
        tx.getCommands().stream().forEach(command -> {
            CommandData commandData = command.getValue();
            if (commandData instanceof InterBankTransferContract.InterBankTransferCommands.BankTransfer)
                verifyAtomicExchange(tx);
        });
    }

    private void verifyAtomicExchange(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("No InterBank Transfer State should be consumed in a transfer",
                    tx.inputsOfType(InterBankTransferState.class).isEmpty());
            require.using("Exactly 1 InterBank Transfer State should be created in a transfer",
                    tx.outputsOfType(InterBankTransferState.class).size() == 1);
            List<DDRObjectState> inputs = tx.inputsOfType(DDRObjectState.class);
            List<DDRObjectState> outputs = tx.outputsOfType(DDRObjectState.class);
            require.using("DDR Objects should be consumed in an Atomic Exchange", !inputs.isEmpty());
            require.using("DDR Objects should be created in an Atomic Exchange", !outputs.isEmpty());
            require.using("All DDR Objects consumed should have the same owner", doDDRsHaveSameOwner(inputs));
            return null;
        });
    }

    private boolean doDDRsHaveSameOwner(List<DDRObjectState> ddrs){
        Party owner = (Party) ddrs.get(0).getOwner();
        return ddrs.stream().allMatch(ddr -> ddr.getOwner().equals(owner));
    }

}
