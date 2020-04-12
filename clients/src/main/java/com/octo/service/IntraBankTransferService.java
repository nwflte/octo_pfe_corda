package com.octo.service;

import com.octo.dto.BankTransferDTO;
import com.octo.schemas.PersistentIntraBankTransfer;
import net.corda.core.transactions.SignedTransaction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public interface IntraBankTransferService {

    List<PersistentIntraBankTransfer> loadAll();

    Optional<PersistentIntraBankTransfer> findById(String id);

    SignedTransaction transfer(BankTransferDTO dto) throws ExecutionException, InterruptedException;

}
