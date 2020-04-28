package com.octo.web;

import com.octo.dto.ObligationRequestDTO;
import com.octo.dto.ObligationUpdateDTO;
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
@RequestMapping(value = "api/obligations") // The paths for HTTP requests are relative to this base path.
public class ObligationController {
    private static final Logger logger = LoggerFactory.getLogger(ObligationController.class);

    @Autowired
    private ObligationService obligationService;

    /**
     * Displays all ddr Obligations states that exist in the node's vault.
     */
    @GetMapping
    public List<PersistentDDRObligation> getAllObligations() {
        return obligationService.loadAll(Vault.StateStatus.ALL);
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

    @PostMapping(value = "request-pledge", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> requestPledge(@RequestBody ObligationRequestDTO request) throws InterruptedException, ExecutionException {
        SignedTransaction signedTx = obligationService.createPledge(request.getAmount());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

    @PostMapping("cancel-pledge")
    public ResponseEntity<String> cancelPledge(@RequestBody ObligationUpdateDTO request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.cancelPledge(request.getExternalId());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

    @PostMapping("deny-pledge")
    public ResponseEntity<String> denyPledge(@RequestBody ObligationUpdateDTO request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.denyPledge(request.getExternalId());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

    @PostMapping("approve-pledge")
    public ResponseEntity<String> approvePledge(@RequestBody ObligationUpdateDTO request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.approvePledge(request.getExternalId());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

    @PostMapping(value = "request-redeem")
    public ResponseEntity<String> requestRedeem(@RequestBody ObligationRequestDTO request) throws InterruptedException, ExecutionException {
        SignedTransaction signedTx = obligationService.createRedeem(request.getAmount());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

    @PostMapping("cancel-redeem")
    public ResponseEntity<String> cancelRedeem(@RequestBody ObligationUpdateDTO request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.cancelRedeem(request.getExternalId());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

    @PostMapping("deny-redeem")
    public ResponseEntity<String> denyRedeem(@RequestBody ObligationUpdateDTO request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.denyRedeem(request.getExternalId());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

    @PostMapping("approve-redeem")
    public ResponseEntity<String> approveRedeem(@RequestBody ObligationUpdateDTO request) throws ExecutionException, InterruptedException {
        SignedTransaction signedTx = obligationService.approveRedeem(request.getExternalId());
        return ResponseEntity.ok(signedTx.getId().toString());
    }

}