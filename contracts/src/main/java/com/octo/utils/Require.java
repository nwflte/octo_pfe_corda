package com.octo.utils;

public class Require{
        public static void using(String message, Boolean expression){
            if (!expression) throw new IllegalArgumentException("Failed requirement: " + message);
        }
}