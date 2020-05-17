package com.octo.corda_services;

import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;

@CordaService
public class BankComptesOracle extends SingletonSerializeAsToken {

    private final ServiceHub services;

    public BankComptesOracle(ServiceHub services) {
        this.services = services;
    }

    private HttpBankCompteService httpBankCompteService() {
        return services.cordaService(HttpBankCompteService.class);
    }

    public String query(String rib) throws Exception {
        return httpBankCompteService().verifyAccountExists(rib);
    }

}
