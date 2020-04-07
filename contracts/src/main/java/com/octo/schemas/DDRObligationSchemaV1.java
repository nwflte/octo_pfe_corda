package com.octo.schemas;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

import java.util.Collections;

@CordaSerializable
public class DDRObligationSchemaV1 extends MappedSchema {

    public DDRObligationSchemaV1(){
        super(DDRObligationSchemaFamily.class, 1, Collections.singletonList(PersistentDDRObligation.class));
    }


}
