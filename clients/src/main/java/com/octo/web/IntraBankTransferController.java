package com.octo.web;

import com.octo.dto.BankTransferDTO;
import com.octo.exceptions.TransferNotFoundException;
import com.octo.schemas.PersistentIntraBankTransfer;
import com.octo.service.IntraBankTransferService;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController()
@RequestMapping("intra")
public class IntraBankTransferController {

    private static final Logger logger = LoggerFactory.getLogger(IntraBankTransferController.class);

    @Autowired
    private IntraBankTransferService intraBankTransferService;

    @GetMapping
    public ResponseEntity<List<PersistentIntraBankTransfer>> getAllIntraBankTransfers() {
        return ResponseEntity.ok(intraBankTransferService.loadAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<PersistentIntraBankTransfer> getIntraBankTransfer(@PathVariable String id) {
        return ResponseEntity.ok(intraBankTransferService.findById(id).orElseThrow(() -> new TransferNotFoundException(id)));
    }

    @PostMapping(value = "/record-transfer")
    public ResponseEntity<String> recordTransfer(@RequestBody BankTransferDTO dto) throws InterruptedException, ExecutionException {
        SignedTransaction signedTx = intraBankTransferService.transfer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + signedTx.getId() + " committed to ledger.");
    }

}
