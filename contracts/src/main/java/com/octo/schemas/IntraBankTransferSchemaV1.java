package com.octo.schemas;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class IntraBankTransferSchemaV1 extends MappedSchema {

    public IntraBankTransferSchemaV1(){
        super(IntraBankTransferSchemaFamily.class, 1, ImmutableList.of(PersistentIntraBankTransfer.class));
    }
}
