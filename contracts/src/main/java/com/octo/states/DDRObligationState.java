package com.octo.states;

import com.google.common.collect.ImmutableList;
import com.octo.contracts.DDRObligationContract;
import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.Date;
import java.util.List;

@BelongsToContract(DDRObligationContract.class)
public class DDRObligationState implements LinearState, OwnableState {

    private final Party issuer;
    private final Party requester;
    private final Date requesterDate;
    private final Amount<Currency> amount;
    private final Party owner;
    private final DDRObligationType type;
    private final DDRObligationStatus status;
    private final String externalId;
    private final UniqueIdentifier linearId;

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

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(requester, owner);
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

    public Party getOwner() {
        return owner;
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

    public DDRObligationState approveRequest(){
        if(status == DDRObligationStatus.REQUEST)
            return new DDRObligationState(issuer, requester, requesterDate, amount, owner, type, DDRObligationStatus.APPROVED, externalId);
        throw new IllegalStateException("Cannot approve an obligation that's in " + status.toString() + " Status");
    }
}
