package com.octo.states;

import com.google.common.collect.ImmutableList;
import com.octo.contracts.IntraBankTransferContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.List;

@BelongsToContract(IntraBankTransferContract.class)
public class IntraBankTransferState implements LinearState {

    private final String senderRIB;
    private final String receiverRIB;
    private final Party bank;
    private final BigDecimal amount;
    private final Currency currency;
    private final Date executionDate;

    private final UniqueIdentifier linearId;

    public IntraBankTransferState(String senderRIB, String receiverRIB, Party senderBank, BigDecimal amount, Currency currency, Date executionDate, UniqueIdentifier linearId) {
        this.senderRIB = senderRIB;
        this.receiverRIB = receiverRIB;
        this.bank = senderBank;
        this.amount = amount;
        this.currency = currency;
        this.executionDate = executionDate;
        this.linearId = linearId;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(bank);
    }

    public String getSenderRIB() {
        return senderRIB;
    }

    public String getReceiverRIB() {
        return receiverRIB;
    }

    public Party getBank() {
        return bank;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Date getExecutionDate() {
        return executionDate;
    }
}
