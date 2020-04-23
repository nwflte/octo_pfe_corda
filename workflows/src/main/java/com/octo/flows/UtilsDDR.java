package com.octo.flows;

import com.octo.states.DDRObjectState;
import com.octo.states.DDRObjectStateBuilder;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.Amount;

import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class UtilsDDR {
    /**
     * Produces DDR Objects of 10DH, then if there's a rest it produces additional one of rest value
     * Example: For quantity 2500 of tokens (That is 15DH), will produce 3 DDRObjects, two of 10DH and other of 5DH.
     *
     * @return
     */
    public static List<DDRObjectState> produceDDRObjects(DDRObligationState obligationState) {
        Amount<Currency> amount = obligationState.getAmount();
        long quantity = amount.getQuantity(); // For exmaple 1000 token
        DDRObjectStateBuilder builder = new DDRObjectStateBuilder();
        builder.issuer(obligationState.getIssuer()).issuerDate(new Date()).owner(obligationState.getOwner())
                .currency(obligationState.getCurrency());
        if (quantity > 1000) {
            int numberOfTokens = (int) Math.ceil((double) quantity / 1000);
            return amount.splitEvenly(numberOfTokens).stream().map(am -> builder.amount(am.getQuantity()).build()).collect(Collectors.toList());
        }

        return Collections.singletonList(builder.amount(quantity).build());
    }
}
