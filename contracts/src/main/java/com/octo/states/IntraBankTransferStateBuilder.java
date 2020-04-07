package com.octo.states;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.util.Currency;
import java.util.Date;

@CordaSerializable
public class IntraBankTransferStateBuilder {

    String senderRIB;
    String receiverRIB;
    Party bank;
    Amount<Currency> amount;
    Date executionDate;
    String externalId;
    UniqueIdentifier linearId;

    public IntraBankTransferStateBuilder() {

    }

    public IntraBankTransferStateBuilder(IntraBankTransferState state) {
        this.senderRIB = state.getSenderRIB();
        this.receiverRIB = state.getReceiverRIB();
        this.bank = state.getBank();
        this.amount = state.getAmount();
        this.executionDate = state.getExecutionDate();
        this.externalId = state.getExternalId();
        this.linearId = state.getLinearId();
    }


    public IntraBankTransferStateBuilder senderRIB(String senderRIB) {
        this.senderRIB = senderRIB;
        return this;
    }

    public IntraBankTransferStateBuilder receiverRIB(String receiverRIB) {
        this.receiverRIB = receiverRIB;
        return this;
    }

    public IntraBankTransferStateBuilder bank(Party bank) {
        this.bank = bank;
        return this;
    }

    public IntraBankTransferStateBuilder amount(Amount<Currency> amount) {
        this.amount = amount;
        return this;
    }

    public IntraBankTransferStateBuilder executionDate(Date executionDate) {
        this.executionDate = executionDate;
        return this;
    }

    public IntraBankTransferStateBuilder externalId(String externalId) {
        this.externalId = externalId;
        this.linearId = new UniqueIdentifier(externalId);
        return this;
    }

    public IntraBankTransferState build() {
        IntraBankTransferState state = new IntraBankTransferState(this);
        validateState(state);
        return state;
    }

    private void validateState(IntraBankTransferState state) {
        if (state.getAmount() != null && state.getExecutionDate() != null && state.getExternalId() != null && state.getLinearId() != null
                && state.getBank() != null && state.getReceiverRIB() != null && state.getSenderRIB() != null)
            return;
        throw new IllegalArgumentException("IntraBankTransferState cannot have null fields");
    }

}
