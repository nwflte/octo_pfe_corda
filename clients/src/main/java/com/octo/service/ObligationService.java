package com.octo.service;

import com.octo.schemas.PersistentDDRObligation;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public interface ObligationService {

    List<PersistentDDRObligation> loadAll(Vault.StateStatus stateStatus);

    Optional<PersistentDDRObligation> findById(String id);

    SignedTransaction createPledge(long amount) throws ExecutionException, InterruptedException;

    SignedTransaction cancelPledge(String externalId) throws ExecutionException, InterruptedException;

    SignedTransaction createRedeem(long amount) throws ExecutionException, InterruptedException;

    SignedTransaction cancelRedeem(String externalId) throws ExecutionException, InterruptedException;

}
