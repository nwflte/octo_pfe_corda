package com.octo.exceptions;

public class NegativeOrNullAmountException extends RuntimeException {

    private final long amount;

    public NegativeOrNullAmountException(long amount){
        super("Negative or zero amount are not permitted, given amount is :" + amount);
        this.amount = amount;
    }

    public long getAmount() {
        return amount;
    }
}
