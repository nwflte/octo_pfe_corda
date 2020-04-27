package com.octo.services;

import com.octo.states.InterBankTransferState;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;

import java.io.IOException;
import java.util.Currency;

@CordaService
public class BankComptesOracle extends SingletonSerializeAsToken {

    private final ServiceHub services;

    public BankComptesOracle(ServiceHub services) {
        this.services = services;
    }

    private HttpBankCompteService httpBankCompteService() {
        return services.cordaService(HttpBankCompteService.class);
    }

    public boolean verifyAccountExists(String rib, Party bank) throws IOException {
        return httpBankCompteService().verifyAccountExists(rib, bank);
    }

    public boolean verifyAccountEligibleForTransfer(InterBankTransferState transfer, Party verifyingBank) throws IOException {
        return httpBankCompteService().verifyAccountEligibleForTransfer(transfer, verifyingBank);
    }

}
