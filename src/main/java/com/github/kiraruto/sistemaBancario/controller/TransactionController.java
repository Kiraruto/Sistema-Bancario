package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.github.kiraruto.sistemaBancario.utils.VerifyUUID.validateUUID;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> pendantToComplete(@PathVariable("id") String id) {
        var transactionId = validateUUID(id);
        transactionService.completed(transactionId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/failure")
    public ResponseEntity<?> pendantToFailure(@PathVariable("id") String id) {
        transactionService.failure(validateUUID(id));
        return ResponseEntity.ok().build();
    }
}
