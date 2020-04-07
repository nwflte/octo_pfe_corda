package com.octo.schemas;

import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "intrabank_transfer_states")
public class PersistentIntraBankTransfer extends PersistentState implements Serializable {

    @Column(name = "sender_rib")
    private String senderRIB;

    @Column(name = "receiver_rib")
    private String receiverRIB;

    @Column(name = "bank")
    private String bank;

    @Column(name = "amount")
    private long amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "execution_date")
    private Date executionDate;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "linear_id")
    private UUID linearId;

    public PersistentIntraBankTransfer(String senderRIB, String receiverRIB, String bank, long amount,
                                       String currency, Date executionDate, String externalId, UUID linearId) {
        this.senderRIB = senderRIB;
        this.receiverRIB = receiverRIB;
        this.bank = bank;
        this.amount = amount;
        this.currency = currency;
        this.executionDate = executionDate;
        this.externalId = externalId;
        this.linearId = linearId;
    }

    public PersistentIntraBankTransfer() {
    }

    public String getSenderRIB() {
        return senderRIB;
    }

    public String getReceiverRIB() {
        return receiverRIB;
    }

    public String getBank() {
        return bank;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Date getExecutionDate() {
        return executionDate;
    }

    public String getExternalId() {
        return externalId;
    }

    public UUID getLinearId() {
        return linearId;
    }
}