package com.octo.states;

import com.octo.builders.InterBankTransferStateBuilder;
import com.octo.contracts.InterBankTransferContract;
import com.octo.schemas.InterBankTransferSchemaV1;
import com.octo.schemas.PersistentInterBankTransfer;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@BelongsToContract(InterBankTransferContract.class)
public class InterBankTransferState implements LinearState, QueryableState {

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
        return Arrays.asList(senderBank, receiverBank);
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

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if (schema instanceof InterBankTransferSchemaV1)
            return new PersistentInterBankTransfer(senderRIB, receiverRIB, senderBank.toString(), receiverBank.toString(), amount.getQuantity(),
                    amount.getToken().getDisplayName(), executionDate, externalId, linearId.getId());
        else throw new IllegalArgumentException("Unsupported Schema");
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Collections.singletonList(new InterBankTransferSchemaV1());
    }
}
