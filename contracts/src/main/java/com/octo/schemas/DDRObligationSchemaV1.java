package com.octo.schemas;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class DDRObligationSchemaV1 extends MappedSchema {

    public DDRObligationSchemaV1(){
        super(DDRObligationSchemaFamily.class, 1, ImmutableList.of(PersistentDDRObligation.class));
    }


}
