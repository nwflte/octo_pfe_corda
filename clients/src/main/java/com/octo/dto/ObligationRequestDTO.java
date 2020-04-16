package com.octo.dto;

public class ObligationRequestDTO {
    private long amount;


    public ObligationRequestDTO() {
    }

    public ObligationRequestDTO(long amount) {
        this.amount = amount;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
