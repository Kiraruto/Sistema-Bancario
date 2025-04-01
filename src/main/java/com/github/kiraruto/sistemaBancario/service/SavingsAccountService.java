package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionScheduleRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.service.interfaces.TransactionService;
import com.github.kiraruto.sistemaBancario.utils.SavingsAccountValidate;
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
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountValidate savingsAccountValidate;
    private final TransactionRepository transactionRespository;
    private final TransactionValidate transactionValidate;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final TransactionScheduleRepository transactionScheduleRepository;
    private final FraudDetectionService fraudDetectionService;
    private final BankHoursService bankHoursService;

    public List<SavingsAccount> getAll() {
        return savingsAccountRepository.findAll();
    }

    public SavingsAccount getById(UUID id) {
        return savingsAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe!"));
    }

    public SavingsAccount createAccount(SavingsAccountDTO savingsAccountDTO) {
        savingsAccountValidate.validateCreateAccountSavings(savingsAccountDTO);

        User user = userRepository.findById(savingsAccountDTO.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + savingsAccountDTO.userId()));

        SavingsAccount savingsAccount = new SavingsAccount(savingsAccountDTO);
        savingsAccount.setUser(user);

        savingsAccountRepository.save(savingsAccount);

        Optional<SavingsAccount> saOpt = savingsAccountRepository.findByCpfAndEmail(savingsAccountDTO.cpf(), savingsAccountDTO.email());
        user.setSavingsAccount(saOpt.orElseThrow(() -> new IllegalArgumentException("Não existe conta com este Cpf e este Email")));
        userRepository.save(user);

        return savingsAccount;
    }

    public void transferSavingsAccount(DepositRequestDTO depositRequestDTO) {
        savingsAccountValidate.validateDepositSavings(depositRequestDTO);

        Optional<SavingsAccount> saOpt1 = savingsAccountRepository.findById(depositRequestDTO.accountSends());
        if (saOpt1.isEmpty()) {
            throw new RuntimeException("Conta com este id não existe");
        }

        Optional<SavingsAccount> saOpt2 = savingsAccountRepository.findById(depositRequestDTO.accountReceive());
        if (saOpt2.isEmpty()) {
            throw new RuntimeException("Conta com este id não existe");
        }

        SavingsAccount sa1 = saOpt1.get();
        SavingsAccount sa2 = saOpt2.get();

        BigDecimal newBalance1 = sa1.getBalance().subtract(depositRequestDTO.balance());
        BigDecimal newBalance2 = sa2.getBalance().add(depositRequestDTO.balance());

        sa1.setBalance(newBalance1);
        sa2.setBalance(newBalance2);

        savingsAccountRepository.save(sa1);
        savingsAccountRepository.save(sa2);

        Transaction transaction = new Transaction(depositRequestDTO, transactionValidate);
        transactionRespository.save(transaction);
    }


    public BalanceDTO getBalance(UUID uuid) {
        BalanceDTO fullNameAndBalanceById = savingsAccountRepository.findFullNameAndBalanceById(uuid);

        if (!fullNameAndBalanceById.isActive()) {
            throw new IllegalArgumentException("A conta com este id não está ativa");
        }

        return fullNameAndBalanceById;
    }

    public void disableSavingsAccount(UUID uuid) {
        SavingsAccount savingsAccount = savingsAccountValidate.validateSavingsAccountToFalse(uuid);

        savingsAccount.setIsActive(false);
        savingsAccountRepository.save(savingsAccount);
    }

    public void activateSavingsAccount(UUID uuid) {
        SavingsAccount savingsAccount = savingsAccountValidate.validateSavingsAccountToTrue(uuid);

        savingsAccount.setIsActive(true);
        savingsAccountRepository.save(savingsAccount);
    }

    public List<TransactionDTO> getTransactions(UUID uuid) {
        if (!savingsAccountRepository.existsById(uuid)) {
            throw new IllegalArgumentException("A conta com este id não está ativa");
        }

        List<Transaction> transactions = transactionRespository.findByAccountSends(uuid);

        return transactions.stream()
                .map(TransactionDTO::new)
                .toList();
    }

    public void withdraw(UUID uuid, WithdrawRequestDTO withdrawRequestDTO) {
        SavingsAccount validateSavingsAccount = savingsAccountValidate.validateSavingsAccountWithdraw(withdrawRequestDTO);

        SavingsAccount savingsAccount = savingsAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta com este id não existe"));

        Transaction transaction = new Transaction(withdrawRequestDTO, transactionValidate);
        transactionRespository.save(transaction);

        var balance = validateSavingsAccount.getBalance().add(withdrawRequestDTO.amount());

        savingsAccount.setBalance(balance);
        savingsAccountRepository.save(savingsAccount);
    }

    public void withdrawal(UUID uuid, @Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        SavingsAccount validateSavingsAccount = savingsAccountValidate.validateSavingsAccountWithdrawal(withdrawalRequestDTO);

        SavingsAccount savingsAccount = savingsAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta com este id não existe"));

        Transaction transaction = new Transaction(withdrawalRequestDTO);
        transactionRespository.save(transaction);


        var balance = validateSavingsAccount.getBalance().subtract(withdrawalRequestDTO.amount());
        savingsAccount.setBalance(balance);
        savingsAccountRepository.save(savingsAccount);
    }
}
