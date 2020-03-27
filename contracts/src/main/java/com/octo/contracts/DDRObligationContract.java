package com.octo.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class DDRObligationContract implements Contract {

    public static final String ID = "com.octo.contracts.DDRObligationContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }


    public interface DDRObligationCommands extends CommandData {
        class RequestDDRPledge implements DDRObligationCommands {}
        class ApproveDDRPledge implements DDRObligationCommands {}
        class DenyDDRPledge implements DDRObligationCommands {}
        class RequestDDRRedeem implements DDRObligationCommands {}
        class ApproveDDRRedeem implements DDRObligationCommands {}
        class DenyDDRRedeem implements DDRObligationCommands {}
    }
}
