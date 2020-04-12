package com.octo.web;

import com.octo.exceptions.ObligationNotFoundException;
import com.octo.schemas.PersistentDDRObligation;
import com.octo.service.ObligationService;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("api/obligations") // The paths for HTTP requests are relative to this base path.
public class ObligationController {
    private static final Logger logger = LoggerFactory.getLogger(ObligationController.class);

    @Autowired
    private ObligationService obligationService;

    /**
     * Displays all ddr Obligations states that exist in the node's vault.
     */
    @GetMapping
    public ResponseEntity<List<PersistentDDRObligation>> getAllObligations() {
        return ResponseEntity.ok(obligationService.loadAll(Vault.StateStatus.ALL));
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<PersistentDDRObligation> getObligations(@PathVariable String id) {
        return ResponseEntity.ok(obligationService.findById(id).orElseThrow(() -> new ObligationNotFoundException(id)));
    }

    @GetMapping(value = "all-consumed")
    public ResponseEntity<List<PersistentDDRObligation>> getAllConsumedObligations() {
        return ResponseEntity.ok(obligationService.loadAll(Vault.StateStatus.CONSUMED));
    }

    @GetMapping(value = "all-unconsumed")
    public ResponseEntity<List<PersistentDDRObligation>> getAllUnconsumedObligations() {
        return ResponseEntity.ok(obligationService.loadAll(Vault.StateStatus.UNCONSUMED));
    }

    @PostMapping(value = "request-pledge")
    public ResponseEntity<String> requestPledge(@RequestBody Map<String, String> request) throws InterruptedException, ExecutionException {
        long amount = Long.parseLong(request.get("amount"));
        SignedTransaction signedTx = obligationService.createPledge(amount);
        return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + signedTx.getId() + " committed to ledger.");
    }

    @PostMapping("cancel-pledge")
    public ResponseEntity<String> cancelPledge(@RequestBody Map<String, String> request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.cancelPledge(request.getOrDefault("id", "defaultid"));
        return ResponseEntity.status(HttpStatus.OK).body("Transaction id " + signedTx.getId() + " committed to ledger.");
    }

    @PostMapping(value = "request-redeem")
    public ResponseEntity<String> requestRedeem(@RequestBody Map<String, String> request) throws InterruptedException, ExecutionException {
        long amount = Long.parseLong(request.get("amount"));
        SignedTransaction signedTx = obligationService.createRedeem(amount);
        return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + signedTx.getId() + " committed to ledger.");
    }

    @PostMapping("cancel-redeem")
    public ResponseEntity<String> cancelRedeem(@RequestBody Map<String, String> request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.cancelRedeem(request.getOrDefault("id", "defaultid"));
        return ResponseEntity.status(HttpStatus.OK).body("Transaction id " + signedTx.getId() + " committed to ledger.");
    }


}