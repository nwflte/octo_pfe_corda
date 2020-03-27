package com.octo.contracts;

import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class InterBankTransferContract implements Contract {

    public static final String ID = "com.octo.contracts.InterBankTransferContract";


    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }
}
