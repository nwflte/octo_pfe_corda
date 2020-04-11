package com.octo;

import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Collections;

public class QueryUtils {

    public static QueryCriteria linearStateByExternalId(String externalId){
        return new QueryCriteria.LinearStateQueryCriteria().withExternalId(Collections.singletonList(externalId));
    }

    public static QueryCriteria withStatusStates(Vault.StateStatus stateStatus){
        return new QueryCriteria.VaultQueryCriteria().withStatus(stateStatus);
    }


}
