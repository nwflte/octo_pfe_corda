package com.octo.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class DDRObjectContract implements Contract {

    public static final String ID = "com.octo.contracts.DDRObjectContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }

    interface DDRObjectCommands extends CommandData {
        class AtomicExchangeDDR implements DDRObjectCommands {}
    }
}
