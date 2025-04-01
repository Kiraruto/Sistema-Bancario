package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.interfaces.TransactionValidate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckingAccountService {

    private final CheckingAccountRepository checkingAccountRepository;
    private final CheckingAccountValidate checkingAccountValidate;
    private final TransactionRepository transactionRespository;
    private final TransactionValidate transactionValidate;
    private final UserRepository userRepository;

    public List<CheckingAccount> getAll() {
        return checkingAccountRepository.findAll();
    }

    public CheckingAccount createAccount(CheckingAccountDTO checkingAccountDTO) {
        checkingAccountValidate.validateCreateAccountChecking(checkingAccountDTO);

        User user = userRepository.findById(checkingAccountDTO.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + checkingAccountDTO.userId()));

        CheckingAccount checkingAccount = new CheckingAccount(checkingAccountDTO);
        checkingAccount.setUser(user);

        checkingAccountRepository.save(checkingAccount);

        Optional<CheckingAccount> chOpt = checkingAccountRepository.findByCpfAndEmail(checkingAccountDTO.cpf(), checkingAccountDTO.email());
        user.setCheckingAccount(chOpt.orElseThrow(() -> new IllegalArgumentException("Não existe conta com este Cpf e este Email")));
        userRepository.save(user);

        return checkingAccount;
    }

    public CheckingAccount getById(UUID id) {
        return checkingAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("O id da transação não foi encontrado"));
    }

    public List<TransactionDTO> getTransactions(UUID uuid) {
        if (!checkingAccountRepository.existsById(uuid)) {
            throw new IllegalArgumentException("A conta com este id não está ativa");
        }

        List<Transaction> transactions = transactionRespository.findByAccountSends(uuid);

        return transactions.stream()
                .map(TransactionDTO::new)
                .toList();
    }

    public BalanceDTO getBalance(UUID uuid) {
        BalanceDTO fullNameAndBalanceById = checkingAccountRepository.findFullNameAndBalanceById(uuid);

        if (!fullNameAndBalanceById.isActive()) {
            throw new IllegalArgumentException("A conta com este id não está ativa");
        }

        return fullNameAndBalanceById;
    }

    public void withdraw(UUID uuid, @Valid WithdrawRequestDTO withdrawRequestDTO) {
        CheckingAccount validateCheckingAccount = checkingAccountValidate.validateCheckingAccountWithdraw(withdrawRequestDTO);

        CheckingAccount checkingAccount = checkingAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta com este id não existe"));

        Transaction transaction = new Transaction(withdrawRequestDTO, transactionValidate);
        transactionRespository.save(transaction);

        var balance = validateCheckingAccount.getBalance().add(withdrawRequestDTO.amount());

        checkingAccount.setBalance(balance);
        checkingAccountRepository.save(checkingAccount);
    }

    public void transferCheckingAccount(@Valid DepositRequestDTO depositRequestDTO) {
        checkingAccountValidate.validateDepositChecking(depositRequestDTO);

        Optional<CheckingAccount> chOpt1 = checkingAccountRepository.findById(depositRequestDTO.accountSends());
        if (chOpt1.isEmpty()) {
            throw new RuntimeException("Conta com este id não existe");
        }

        Optional<CheckingAccount> chOpt2 = checkingAccountRepository.findById(depositRequestDTO.accountReceive());
        if (chOpt2.isEmpty()) {
            throw new RuntimeException("Conta com este id não existe");
        }

        CheckingAccount ch1 = chOpt1.get();
        CheckingAccount ch2 = chOpt2.get();

        BigDecimal newBalance1 = ch1.getBalance().subtract(depositRequestDTO.balance());
        BigDecimal newBalance2 = ch2.getBalance().add(depositRequestDTO.balance());

        ch1.setBalance(newBalance1);
        ch2.setBalance(newBalance2);

        checkingAccountRepository.save(ch1);
        checkingAccountRepository.save(ch2);

        Transaction transaction = new Transaction(depositRequestDTO, transactionValidate);
        transaction.setDescription("Deposito Conta Corrente");
        transactionRespository.save(transaction);
    }

    public void disableCheckingAccount(UUID uuid) {
        CheckingAccount checkingAccount = checkingAccountValidate.validateCheckingAccountToFalse(uuid);

        checkingAccount.setIsActive(false);
        checkingAccountRepository.save(checkingAccount);
    }

    public void activateCheckingAccount(UUID uuid) {
        CheckingAccount checkingAccount = checkingAccountValidate.validateCheckingAccountToTrue(uuid);

        checkingAccount.setIsActive(true);
        checkingAccountRepository.save(checkingAccount);
    }

    public void withdrawal(UUID uuid, @Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        CheckingAccount validateCheckingAccount = checkingAccountValidate.validateCheckingAccountWithdrawal(withdrawalRequestDTO);

        CheckingAccount checkingAccount = checkingAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta com este id não existe"));

        Transaction transaction = new Transaction(withdrawalRequestDTO);
        transaction.setDescription("Saque Conta Pounpança");
        transactionRespository.save(transaction);


        var balance = validateCheckingAccount.getBalance().subtract(withdrawalRequestDTO.amount());
        checkingAccount.setBalance(balance);
        checkingAccountRepository.save(checkingAccount);
    }
}
