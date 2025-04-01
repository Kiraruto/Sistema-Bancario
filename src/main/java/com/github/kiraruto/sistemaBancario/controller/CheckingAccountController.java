package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.service.CheckingAccountService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.github.kiraruto.sistemaBancario.utils.VerifyUUID.validateUUID;

@RestController
@RequestMapping("/checking-accounts")
@RequiredArgsConstructor
public class CheckingAccountController {

    private final CheckingAccountService checkingAccountService;

    @GetMapping
    public ResponseEntity<List<CheckingAccount>> getAllCheckingAccounts() {
        List<CheckingAccount> all = checkingAccountService.getAll();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckingAccount> getById(@PathVariable String id) {
        CheckingAccount account = checkingAccountService.getById(validateUUID(id));
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable String id) {
        List<TransactionDTO> transactions = checkingAccountService.getTransactions(validateUUID(id));
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceDTO> getBalance(@PathVariable String id) {
        BalanceDTO balance = checkingAccountService.getBalance(validateUUID(id));
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<CheckingAccount> createAccountCh(@RequestBody @Valid CheckingAccountDTO checkingAccountDTO) {
        CheckingAccount ch = checkingAccountService.createAccount(checkingAccountDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ch);
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String id, @RequestBody @Valid WithdrawRequestDTO withdrawRequestDTO) {
        checkingAccountService.withdraw(validateUUID(id), withdrawRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/withdrawal")
    public ResponseEntity<Void> withdrawal(@PathVariable String id, @RequestBody @Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        checkingAccountService.withdrawal(validateUUID(id), withdrawalRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/transfer/checkingAccount")
    public ResponseEntity<CheckingAccount> transferRequest(@RequestBody @Valid DepositRequestDTO depositRequestDTO) {
        checkingAccountService.transferCheckingAccount(depositRequestDTO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/{id}/disableCheckingAccount")
    public ResponseEntity<Void> disableCheckingAccount(@PathVariable("id") String id) {
        checkingAccountService.disableCheckingAccount(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activateCheckingAccount")
    public ResponseEntity<Void> activateCheckingAccount(@PathVariable("id") String id) {
        checkingAccountService.activateCheckingAccount(validateUUID(id));
        return ResponseEntity.noContent().build();
    }
}
