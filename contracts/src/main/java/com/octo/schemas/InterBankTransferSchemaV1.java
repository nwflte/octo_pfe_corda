package com.octo.schemas;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class InterBankTransferSchemaV1 extends MappedSchema {

    public InterBankTransferSchemaV1(){
        super(InterBankTransferSchemaFamily.class, 1, ImmutableList.of(PersistentInterBankTransfer.class));
    }
}
