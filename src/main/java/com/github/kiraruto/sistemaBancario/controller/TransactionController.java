package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.github.kiraruto.sistemaBancario.utils.VerifyUUID.validateUUID;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<List<Transaction>> getAllTransactionPendente(@PathVariable("id") String id) {
        List<Transaction> transactions = transactionService.getAllTransactionPendente(validateUUID(id));
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/full/{id}")
    public ResponseEntity<Void> pendantToFull(@PathVariable("id") String id) {
        transactionService.completed(validateUUID(id));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/failure/{id}")
    public ResponseEntity<Void> pendantToFailure(@PathVariable("id") String id) {
        transactionService.failure(validateUUID(id));
        return ResponseEntity.ok().build();
    }
}
