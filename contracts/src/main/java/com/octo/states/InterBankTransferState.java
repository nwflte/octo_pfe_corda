package com.octo.states;

import com.google.common.collect.ImmutableList;
import com.octo.contracts.InterBankTransferContract;
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

@BelongsToContract(InterBankTransferContract.class)
public class InterBankTransferState implements LinearState {

    private final String senderRIB;
    private final String receiverRIB;
    private final Party senderBank;
    private final Party receiverBank;
    private final BigDecimal amount;
    private final Currency currency;
    private final Date executionDate;

    private final UniqueIdentifier linearId;

    public InterBankTransferState(String senderRIB, String receiverRIB, Party senderBank, Party receiverBank, BigDecimal amount, Currency currency, Date executionDate, UniqueIdentifier linearId) {
        this.senderRIB = senderRIB;
        this.receiverRIB = receiverRIB;
        this.senderBank = senderBank;
        this.receiverBank = receiverBank;
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
        return ImmutableList.of(senderBank, receiverBank);
    }

    public String getSenderRIB() {
        return senderRIB;
    }

    public String getReceiverRIB() {
        return receiverRIB;
    }

    public Party getSenderBank() {
        return senderBank;
    }

    public Party getReceiverBank() {
        return receiverBank;
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
