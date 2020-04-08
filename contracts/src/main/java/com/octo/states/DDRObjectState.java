package com.octo.states;

import com.octo.contracts.DDRObjectContract;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.List;

@BelongsToContract(DDRObjectContract.class)
public class DDRObjectState implements FungibleState<Currency>, OwnableState {

    private final Party issuer;
    private final Date issuerDate;
    private final Amount<Currency> amount;
    private final Party owner;

    @ConstructorForDeserialization
    public DDRObjectState(Party issuer, Date issuerDate, Amount<Currency> amount, Party owner) {
        this.issuer = issuer;
        this.issuerDate = issuerDate;
        this.amount = amount;
        this.owner = owner;
    }

    public DDRObjectState(DDRObjectStateBuilder builder) {
        this.issuer = builder.issuer;
        this.issuerDate = builder.issuerDate;
        this.amount = new Amount<>(builder.amount, builder.currency);
        this.owner = builder.owner;
    }

    @NotNull
    @Override
    public Amount<Currency> getAmount() {
        return amount;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer, owner);
    }

    @NotNull
    @Override
    public AbstractParty getOwner() {
        return owner;
    }

    @NotNull
    @Override
    public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
        return new CommandAndState(null, new DDRObjectState(issuer, issuerDate, amount, (Party) newOwner));
    }

    public Party getIssuer() {
        return issuer;
    }

    public Date getIssuerDate() {
        return issuerDate;
    }

    public Currency getCurrency() {
        return amount.getToken();
    }
}
