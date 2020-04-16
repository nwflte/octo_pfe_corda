package com.octo.utils;

import net.corda.core.contracts.Amount;

import java.util.Currency;

public class DDRUtils {
    public static Amount<Currency> buildDirham(long amount){
        return Amount.parseCurrency(String.valueOf(amount) + " MAD");
    }
}
