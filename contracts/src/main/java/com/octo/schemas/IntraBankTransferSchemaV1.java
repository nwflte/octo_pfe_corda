package com.octo.schemas;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

import java.util.Collections;

@CordaSerializable
public class IntraBankTransferSchemaV1 extends MappedSchema {

    public IntraBankTransferSchemaV1(){
        super(IntraBankTransferSchemaFamily.class, 1, Collections.singletonList(PersistentIntraBankTransfer.class));
    }
}
