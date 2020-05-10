package com.octo.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class BankTransferDTO implements Serializable {
    private String reference;
    private String senderRIB;
    private String receiverRIB;
    private BigDecimal amount;
    private Date executionDate;
    private String status;
    private Date statusUpdate;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStatusUpdate() {
        return statusUpdate;
    }

    public void setStatusUpdate(Date statusUpdate) {
        this.statusUpdate = statusUpdate;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setSenderRIB(String senderRIB) {
        this.senderRIB = senderRIB;
    }

    public void setReceiverRIB(String receiverRIB) {
        this.receiverRIB = receiverRIB;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setExecutionDate(Date executionDate) {
        this.executionDate = executionDate;
    }

    public String getSenderRIB() {
        return senderRIB;
    }

    public String getReceiverRIB() {
        return receiverRIB;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Date getExecutionDate() {
        return executionDate;
    }

    @Override
    public String toString() {
        return "BankTransferDTO{" +
                "reference='" + reference + '\'' +
                ", senderRIB='" + senderRIB + '\'' +
                ", receiverRIB='" + receiverRIB + '\'' +
                ", amount=" + amount +
                ", executionDate=" + executionDate +
                ", status=" + status +
                ", statusUpdate=" + statusUpdate +
                '}';
    }
}
