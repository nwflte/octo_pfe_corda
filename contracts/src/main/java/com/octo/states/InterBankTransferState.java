package com.octo.states;

import com.google.common.collect.ImmutableList;
import com.octo.contracts.InterBankTransferContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.Date;
import java.util.List;

@BelongsToContract(InterBankTransferContract.class)
public class InterBankTransferState implements LinearState {

    private final String senderRIB;
    private final String receiverRIB;
    private final Party senderBank;
    private final Party receiverBank;
    private final Amount<Currency> amount;
    private final Date executionDate;

    private final String externalId;
    private final UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public InterBankTransferState(String senderRIB, String receiverRIB, Party senderBank, Party receiverBank, Amount<Currency> amount,
                                  Date executionDate, String externalId) {
        this.senderRIB = senderRIB;
        this.receiverRIB = receiverRIB;
        this.senderBank = senderBank;
        this.receiverBank = receiverBank;
        this.amount = amount;
        this.executionDate = executionDate;
        this.externalId = externalId;
        this.linearId = new UniqueIdentifier(externalId);
    }

    public InterBankTransferState(InterBankTransferStateBuilder builder) {
        this.senderRIB = builder.senderRIB;
        this.receiverRIB = builder.receiverRIB;
        this.senderBank = builder.senderBank;
        this.receiverBank = builder.receiverBank;
        this.amount = builder.amount;
        this.executionDate = builder.executionDate;
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

    public Amount<Currency> getAmount() {
        return amount;
    }

    public Date getExecutionDate() {
        return executionDate;
    }

    public String getExternalId() {
        return externalId;
    }
}
