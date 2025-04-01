package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.dto.CheckingAccountDTO;
import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.TransactionDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.service.CheckingAccountService;
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

    @PostMapping
    public ResponseEntity<CheckingAccount> createAccountCh(@RequestBody CheckingAccountDTO checkingAccountDTO) {
        CheckingAccount ch = checkingAccountService.createAccount(checkingAccountDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ch);
    }

    @PutMapping
    public ResponseEntity<CheckingAccount> DepositRequest(@RequestBody DepositRequestDTO depositRequestDTO) {
        checkingAccountService.depositCheckingAccount(depositRequestDTO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
