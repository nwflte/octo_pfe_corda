package com.octo.states;

import com.octo.contracts.IntraBankTransferContract;
import com.octo.schemas.IntraBankTransferSchemaV1;
import com.octo.schemas.PersistentIntraBankTransfer;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;

@BelongsToContract(IntraBankTransferContract.class)
public class IntraBankTransferState implements LinearState, QueryableState {

    private final String senderRIB;
    private final String receiverRIB;
    private final Party bank;
    private final Amount<Currency> amount;
    private final Date executionDate;

    private final String externalId;
    private final UniqueIdentifier linearId;

    public IntraBankTransferState(String senderRIB, String receiverRIB, Party senderBank, Amount<Currency> amount, Date executionDate, String externalId, UniqueIdentifier linearId) {
        this.senderRIB = senderRIB;
        this.receiverRIB = receiverRIB;
        this.bank = senderBank;
        this.amount = amount;
        this.executionDate = executionDate;
        this.externalId = externalId;
        this.linearId = linearId;
    }

    public IntraBankTransferState(IntraBankTransferStateBuilder builder) {
        this.senderRIB = builder.senderRIB;
        this.receiverRIB = builder.receiverRIB;
        this.bank = builder.bank;
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
        return Collections.singletonList(bank);
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

    public Amount<Currency> getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return amount.getToken();
    }

    public Date getExecutionDate() {
        return executionDate;
    }

    public String getExternalId() {
        return externalId;
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if (schema instanceof IntraBankTransferSchemaV1)
            return new PersistentIntraBankTransfer(senderRIB, receiverRIB, bank.toString(), amount.getQuantity(), amount.getToken().getDisplayName(),
                    executionDate, externalId, linearId.getId());
        else throw new IllegalArgumentException("Unsupported Schema");
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Collections.singletonList(new IntraBankTransferSchemaV1());
    }
}
