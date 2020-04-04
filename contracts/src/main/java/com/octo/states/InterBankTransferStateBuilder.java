package com.octo.states;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.util.Currency;
import java.util.Date;

@CordaSerializable
public class InterBankTransferStateBuilder {

     String senderRIB;
     String receiverRIB;
     Party senderBank;
     Party receiverBank;
     Amount<Currency> amount;
     Date executionDate;
     String externalId;
     UniqueIdentifier linearId;

    public InterBankTransferStateBuilder(InterBankTransferState state) {
        this.senderRIB = state.getSenderRIB();
        this.receiverRIB = state.getReceiverRIB();
        this.senderBank = state.getSenderBank();
        this.receiverBank = state.getReceiverBank();
        this.amount = state.getAmount();
        this.executionDate = state.getExecutionDate();
        this.externalId = state.getExternalId();
        this.linearId = state.getLinearId();
    }


    public InterBankTransferStateBuilder senderRIB(String senderRIB) {
        this.senderRIB = senderRIB;
        return this;
    }

    public InterBankTransferStateBuilder receiverRIB(String receiverRIB) {
        this.receiverRIB = receiverRIB;
        return this;
    }

    public InterBankTransferStateBuilder senderBank(Party senderBank) {
        this.senderBank = senderBank;
        return this;
    }

    public InterBankTransferStateBuilder receiverBank(Party receiverBank) {
        this.receiverBank = receiverBank;
        return this;
    }

    public InterBankTransferStateBuilder amount(Amount<Currency> amount) {
        this.amount = amount;
        return this;
    }

    public InterBankTransferStateBuilder executionDate(Date executionDate) {
        this.executionDate = executionDate;
        return this;
    }

    public InterBankTransferStateBuilder externalId(String externalId) {
        this.externalId = externalId;
        this.linearId = new UniqueIdentifier(externalId);
        return this;
    }

    public InterBankTransferState build() {
        InterBankTransferState state = new InterBankTransferState(this);
        validateState(state);
        return state;
    }

    private void validateState(InterBankTransferState state) {
        if (state.getAmount() != null && state.getExecutionDate() != null && state.getExternalId() != null && state.getLinearId() != null
        && state.getReceiverBank() != null && state.getReceiverRIB() != null && state.getSenderBank() != null && state.getSenderRIB() != null)
            return;
        throw new IllegalArgumentException("DDRObjectState cannot have null fields");
    }
    
}
