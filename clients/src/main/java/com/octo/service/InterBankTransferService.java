package com.octo.service;

import com.octo.dto.BankTransferDTO;
import com.octo.schemas.PersistentInterBankTransfer;
import net.corda.core.transactions.SignedTransaction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public interface InterBankTransferService {

    List<PersistentInterBankTransfer> loadAll();

    Optional<PersistentInterBankTransfer> findById(String id);

    SignedTransaction transfer(BankTransferDTO dto) throws ExecutionException, InterruptedException;

}
