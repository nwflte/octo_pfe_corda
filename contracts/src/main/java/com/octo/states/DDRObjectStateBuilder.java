package com.octo.states;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;

import java.util.Currency;

public class DDRObjectStateBuilder {

    Party issuer;
    long amount;
    Currency currency;
    Party owner;

    public DDRObjectStateBuilder() {

    }

    public DDRObjectStateBuilder(FungibleToken state) {
        issuer = state.getIssuer();
        amount = state.getAmount().getQuantity();
        owner = (Party) state.getHolder();
        currency = Currency.getInstance( state.getAmount().getToken().getTokenType().getTokenIdentifier());
    }

    public DDRObjectStateBuilder issuer(Party issuer) {
        this.issuer = issuer;
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

    public FungibleToken build() {
        TokenType token = FiatCurrency.Companion.getInstance(currency.getCurrencyCode());

        IssuedTokenType issuedTokenType = new IssuedTokenType(issuer, token);

        //specify how much amount to issue to holder
        Amount<IssuedTokenType> amount = AmountUtilitiesKt.amount(this.amount, issuedTokenType);

        //create fungible amount specifying the new owner
        return new FungibleToken(amount, owner, TransactionUtilitiesKt.getAttachmentIdForGenericParam(token));
    }

}
