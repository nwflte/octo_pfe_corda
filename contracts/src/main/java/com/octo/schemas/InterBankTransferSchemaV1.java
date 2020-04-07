package com.octo.schemas;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

import java.util.Collections;

@CordaSerializable
public class InterBankTransferSchemaV1 extends MappedSchema {

    public InterBankTransferSchemaV1() {
        super(InterBankTransferSchemaFamily.class, 1, Collections.singletonList(PersistentInterBankTransfer.class));
    }
}
