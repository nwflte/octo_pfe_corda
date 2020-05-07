package com.octo.enums;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public enum DDRObligationStatus {
    APPROVED, REQUEST, REJECTED, CANCELED
}
