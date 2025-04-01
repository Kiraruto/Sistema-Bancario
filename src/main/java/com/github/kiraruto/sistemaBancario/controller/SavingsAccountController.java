package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.service.SavingsAccountService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.github.kiraruto.sistemaBancario.utils.VerifyUUID.validateUUID;

@RestController
@RequestMapping("/savings-accounts")
@RequiredArgsConstructor
public class SavingsAccountController {

    private final SavingsAccountService savingsAccountService;

    @GetMapping
    public ResponseEntity<List<SavingsAccount>> getAllSavingsAccount() {
        List<SavingsAccount> all = savingsAccountService.getAll();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsAccount> getById(@PathVariable String id) {
        SavingsAccount account = savingsAccountService.getById(validateUUID(id));
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable String id) {
        List<TransactionDTO> transactions = savingsAccountService.getTransactions(validateUUID(id));
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceDTO> getBalance(@PathVariable String id) {
        BalanceDTO balance = savingsAccountService.getBalance(validateUUID(id));
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<SavingsAccount> createAccountSa(@RequestBody @Valid SavingsAccountDTO savingsAccountDTO) {
        SavingsAccount sa = savingsAccountService.createAccount(savingsAccountDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(sa);
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String id, @RequestBody @Valid WithdrawRequestDTO withdrawRequestDTO) {
        savingsAccountService.withdraw(validateUUID(id), withdrawRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/transfer/savingsAccount")
    public ResponseEntity<SavingsAccount> transferRequest(@RequestBody @Valid DepositRequestDTO depositRequestDTO) {
        savingsAccountService.transferSavingsAccount(depositRequestDTO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/{id}/disableSavingsAccount")
    public ResponseEntity<Void> disableSavingsAccount(@PathVariable("id") String id) {
        savingsAccountService.disableSavingsAccount(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activateSavingsAccount")
    public ResponseEntity<Void> activateSavingsAccount(@PathVariable("id") String id) {
        savingsAccountService.activateSavingsAccount(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/withdrawal")
    public ResponseEntity<Void> withdrawal(@PathVariable String id, @RequestBody @Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        savingsAccountService.withdrawal(validateUUID(id), withdrawalRequestDTO);
        return ResponseEntity.noContent().build();
    }
}
