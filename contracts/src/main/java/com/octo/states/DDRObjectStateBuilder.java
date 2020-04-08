package com.octo.states;

import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;

import java.util.Currency;
import java.util.Date;

public class DDRObjectStateBuilder {

    Party issuer;
    Date issuerDate;
    long amount;
    Currency currency;
    Party owner;

    public DDRObjectStateBuilder() {

    }

    public DDRObjectStateBuilder(DDRObjectState state) {
        issuer = state.getIssuer();
        issuerDate = state.getIssuerDate();
        amount = state.getAmount().getQuantity();
        owner = (Party) state.getOwner();
        currency = state.getAmount().getToken();
    }

    public DDRObjectStateBuilder issuer(Party issuer) {
        this.issuer = issuer;
        return this;
    }

    public DDRObjectStateBuilder issuerDate(Date issuerDate) {
        this.issuerDate = issuerDate;
        return this;
    }

    public DDRObjectStateBuilder amount(long amount) {
        this.amount = amount;
        return this;
    }

    public DDRObjectStateBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public DDRObjectStateBuilder owner(Party owner) {
        this.owner = owner;
        return this;
    }

    public DDRObjectState build() {
        DDRObjectState ddrObjectState = new DDRObjectState(this);
        validateDDRObjectState(ddrObjectState);
        return ddrObjectState;
    }

    private void validateDDRObjectState(DDRObjectState state) {
        if (state.getIssuer() != null && state.getIssuerDate() != null &&state.getAmount() != null && state.getOwner() != null)
            return;
        throw new IllegalArgumentException("DDRObjectState cannot have null fields");
    }
}
