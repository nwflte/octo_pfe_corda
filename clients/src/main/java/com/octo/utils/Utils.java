package com.octo.utils;

import java.math.BigDecimal;

public class Utils {
    public static long toCentimes(BigDecimal value){
        return value.movePointRight(2).longValue();
    }
}
