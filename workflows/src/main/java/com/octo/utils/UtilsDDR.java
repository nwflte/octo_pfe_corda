package com.octo.utils;

import com.octo.builders.DDRObjectStateBuilder;
import com.octo.states.DDRObjectState;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.Amount;

import java.util.Currency;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class UtilsDDR {
    /**
     * Produces DDR Objects of 10DH, then if there's a rest it produces additional one of rest value
     * Example: For quantity 2500 of tokens (That is 15DH), will produce 3 DDRObjects, two of 10DH and other of 5DH.
     *
     * @return
     */
    /*public static List<DDRObjectState> produceDDRObjects(DDRObligationState obligationState) {
        Amount<Currency> amount = obligationState.getAmount();
        long quantity = amount.getQuantity(); // For exmaple 1000 token
        DDRObjectStateBuilder builder = new DDRObjectStateBuilder();
        builder.issuer(obligationState.getIssuer()).issuerDate(new Date()).owner(obligationState.getOwner())
                .currency(obligationState.getCurrency());

        return quantity <= 1000 ? singletonList(builder.amount(quantity).build()) :
                amount.splitEvenly((int) (quantity/1000)).stream().map(am -> builder.amount(am.getQuantity()).build()).collect(toList());
    }*/

    public static List<DDRObjectState> produceDDRObjects(DDRObligationState obligationState) {
        Amount<Currency> amount = obligationState.getAmount();
        long quantity = amount.getQuantity() / 100; // For exmaple 1000 token
        DDRObjectStateBuilder builder = new DDRObjectStateBuilder();
        builder.issuer(obligationState.getIssuer()).issuerDate(new Date()).owner(obligationState.getOwner())
                .currency(obligationState.getCurrency());

        long numberOfDDR = 1;
        if(quantity >= 10 && quantity <= 999)
            numberOfDDR = quantity / 10;
        else if (quantity > 999){
            String numberAsStr = String.valueOf(quantity).substring(0, 3);
            numberOfDDR = Long.parseLong(numberAsStr);
        }

        return amount.splitEvenly((int) numberOfDDR).stream().map(am -> builder.amount(am.getQuantity()).build()).collect(toList());
    }
}
