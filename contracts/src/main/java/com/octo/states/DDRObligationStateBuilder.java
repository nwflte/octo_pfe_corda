package com.octo.states;

import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.util.Currency;
import java.util.Date;

@CordaSerializable
public class DDRObligationStateBuilder {

    Party issuer;
    Party requester;
    Date requesterDate;
    Amount<Currency> amount;
    Party owner;
    DDRObligationType type;
    DDRObligationStatus status;
    String externalId;
    UniqueIdentifier linearId;

    public DDRObligationStateBuilder() {

    }

    public DDRObligationStateBuilder(DDRObligationState state) {
        issuer = state.getIssuer();
        requester = state.getRequester();
        requesterDate = state.getRequesterDate();
        amount = state.getAmount();
        owner = state.getOwner();
        type = state.getType();
        status = state.getStatus();
        externalId = state.getExternalId();
        linearId = state.getLinearId();
    }

    public DDRObligationStateBuilder issuer(Party issuer) {
        this.issuer = issuer;
        return this;
    }

    public DDRObligationStateBuilder requester(Party requester) {
        this.requester = requester;
        return this;
    }

    public DDRObligationStateBuilder requesterDate(Date requesterDate) {
        this.requesterDate = requesterDate;
        return this;
    }

    public DDRObligationStateBuilder amount(Amount<Currency> amount) {
        this.amount = amount;
        return this;
    }

    public DDRObligationStateBuilder owner(Party owner) {
        this.owner = owner;
        return this;
    }

    public DDRObligationStateBuilder type(DDRObligationType type) {
        this.type = type;
        return this;
    }

    public DDRObligationStateBuilder status(DDRObligationStatus status) {
        this.status = status;
        return this;
    }

    public DDRObligationStateBuilder externalId(String externalId) {
        this.externalId = externalId;
        this.linearId = new UniqueIdentifier(externalId);
        return this;
    }

    public DDRObligationState build() {
        DDRObligationState ddrObligationState = new DDRObligationState(this);
        validateDDRObligationState(ddrObligationState);
        return ddrObligationState;
    }

    private void validateDDRObligationState(DDRObligationState state) {
        if (state.getIssuer() != null && state.getRequester() != null && state.getRequesterDate() != null && state.getAmount() != null
                && state.getOwner() != null && state.getType() != null && state.getStatus() != null && state.getExternalId() != null & state.getLinearId() != null)
            return;
        throw new IllegalArgumentException("DDRObligationState cannot have null fields");
    }
}
