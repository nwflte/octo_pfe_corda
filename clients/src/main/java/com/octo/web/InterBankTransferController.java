package com.octo.web;

import com.octo.dto.BankTransferDTO;
import com.octo.exceptions.TransferNotFoundException;
import com.octo.schemas.PersistentInterBankTransfer;
import com.octo.service.InterBankTransferService;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController()
@RequestMapping("inter")
public class InterBankTransferController {

    private static final Logger logger = LoggerFactory.getLogger(InterBankTransferController.class);

    @Autowired
    private InterBankTransferService interBankTransferService;

    @GetMapping
    public ResponseEntity<List<PersistentInterBankTransfer>> getAllInterBankTransfers() {
        return ResponseEntity.ok(interBankTransferService.loadAll());
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<PersistentInterBankTransfer> getInterBankTransfer(@PathVariable String id) {
        return ResponseEntity.ok(interBankTransferService.findById(id).orElseThrow(() -> new TransferNotFoundException(id)));
    }

    @PostMapping(value = "perform-transfer")
    public ResponseEntity<String> performTransfer(@RequestBody BankTransferDTO dto) throws InterruptedException, ExecutionException {
        SignedTransaction signedTx = interBankTransferService.transfer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + signedTx.getId() + " committed to ledger.");
    }

}
