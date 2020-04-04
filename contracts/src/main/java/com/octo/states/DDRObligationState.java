package com.octo.states;

import com.google.common.collect.ImmutableList;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.Date;
import java.util.List;

@BelongsToContract(DDRObligationContract.class)
public class DDRObligationState implements LinearState, OwnableState {

    private Party issuer;
    private Party requester;
    private Date requesterDate;
    private Amount<Currency> amount;
    private Party owner;
    private DDRObligationType type;
    private DDRObligationStatus status;
    private String externalId;
    private UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public DDRObligationState(Party issuer, Party requester, Date requesterDate, Amount<Currency> amount, Party owner,
                              DDRObligationType type, DDRObligationStatus status, String externalId) {
        this.issuer = issuer;
        this.requester = requester;
        this.requesterDate = requesterDate;
        this.amount = amount;
        this.owner = owner;
        this.type = type;
        this.status = status;
        this.externalId = externalId;
        this.linearId = new UniqueIdentifier(externalId);
    }

    public DDRObligationState(DDRObligationStateBuilder builder) {
        this.issuer = builder.issuer;
        this.requester = builder.requester;
        this.requesterDate = builder.requesterDate;
        this.amount = builder.amount;
        this.owner = builder.owner;
        this.type = builder.type;
        this.status = builder.status;
        this.externalId = builder.externalId;
        this.linearId = builder.linearId;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(requester, owner, issuer);
    }

    public Party getRequester() {
        return requester;
    }

    public Date getRequesterDate() {
        return requesterDate;
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public Currency getCurrency() {return amount.getToken();}

    @NotNull
    public Party getOwner() {
        return owner;
    }

    public Party getIssuer() {
        return issuer;
    }

    public String getExternalId() {
        return externalId;
    }

    public DDRObligationType getType() {
        return type;
    }

    public DDRObligationStatus getStatus() {
        return status;
    }

    @NotNull
    @Override
    public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
        return new CommandAndState(null, new DDRObligationState(issuer, requester, requesterDate, amount, (Party) newOwner,
                type, status, externalId));
    }
}
