package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.TransactionValidate;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CheckingAccountService {

    private static final Logger log = LoggerFactory.getLogger(CheckingAccountService.class);
    private final CheckingAccountRepository checkingAccountRepository;
    private final CheckingAccountValidate checkingAccountValidate;
    private final TransactionRepository transactionRespository;
    private final TransactionValidate transactionValidate;
    private final UserRepository userRepository;
    private final WithdrawalValidatorService withdrawalValidatorService;
    private final DepositValidatorService depositValidatorService;
    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(UUID uuid) {
        return locks.computeIfAbsent(uuid, k -> new ReentrantLock());
    }

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

    @Transactional
    public void deposit(UUID uuid, @Valid WithdrawRequestDTO withdrawRequestDTO) {
        ReentrantLock lock = getLock(uuid);
        lock.lock();
        try {
            checkingAccountValidate.validateCheckingAccountWithdraw(withdrawRequestDTO);

            CheckingAccount checkingAccount = checkingAccountRepository.findById(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("A conta com este id não existe"));

            BigDecimal depositAmount = withdrawRequestDTO.amount();

            depositValidatorService.validateDepositLimit(checkingAccount, depositAmount);
            depositValidatorService.detectSuspiciousDeposits(uuid, checkingAccount, withdrawRequestDTO);
            depositValidatorService.checkFrequentLargeDeposits(uuid, checkingAccount, withdrawRequestDTO, depositAmount);
            depositValidatorService.checkDepositAboveTenThousand(checkingAccount, withdrawRequestDTO, depositAmount);

            Transaction transaction = new Transaction(withdrawRequestDTO, transactionValidate);
            transaction.setDescription("Depósito");
            transactionRespository.save(transaction);

            BigDecimal newBalance = checkingAccount.getBalance().add(depositAmount);
            checkingAccount.setBalance(newBalance);
            checkingAccountRepository.save(checkingAccount);
        } finally {
            lock.unlock();
        }
    }

    public void transferCheckingAccount(@Valid DepositRequestDTO depositRequestDTO) {
        checkingAccountValidate.validateDepositChecking(depositRequestDTO);

        UUID senderId = depositRequestDTO.accountSends();
        UUID receiverId = depositRequestDTO.accountReceive();

        List<UUID> ordered = Stream.of(senderId, receiverId)
                .sorted()
                .toList();

        ReentrantLock firstLock = getLock(ordered.get(0));
        ReentrantLock secondLock = getLock(ordered.get(1));

        firstLock.lock();
        secondLock.lock();
        try {
            CheckingAccount ch1 = checkingAccountRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Conta remetente não existe"));

            CheckingAccount ch2 = checkingAccountRepository.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Conta destinatária não existe"));

            if (depositRequestDTO.balance().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Valor da transferência deve ser positivo");
            }

            if (ch1.getBalance().compareTo(depositRequestDTO.balance()) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente para transferência");
            }

            BigDecimal newBalance1 = ch1.getBalance().subtract(depositRequestDTO.balance());
            BigDecimal newBalance2 = ch2.getBalance().add(depositRequestDTO.balance());

            ch1.setBalance(newBalance1);
            ch2.setBalance(newBalance2);

            checkingAccountRepository.save(ch1);
            checkingAccountRepository.save(ch2);

            Transaction transaction = new Transaction(depositRequestDTO, transactionValidate);
            transaction.setDescription("Transferência entre contas");
            transactionRespository.save(transaction);
        } finally {
            secondLock.unlock();
            firstLock.unlock();
        }
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

    @Transactional
    public void withdrawal(UUID uuid, @Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        ReentrantLock lock = getLock(uuid);
        lock.lock();
        try {
            CheckingAccount checkingAccount = checkingAccountValidate.validateCheckingAccountWithdrawal(withdrawalRequestDTO);

            if (withdrawalRequestDTO.amount().compareTo(checkingAccount.getBalance()) > 0) {
                throw new IllegalArgumentException("Saldo insuficiente para saque");
            }

            withdrawalValidatorService.validateWithdrawalLimit(uuid, withdrawalRequestDTO.amount());

            Transaction transaction = new Transaction(withdrawalRequestDTO);
            transaction.setTransactionType(EnumTransactionType.SAQUE);
            transaction.setDescription("Saque Conta Corrente");
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setAccountReceive(withdrawalRequestDTO.idAccount());
            transaction.setAccountSends(withdrawalRequestDTO.idAccount());
            transactionRespository.save(transaction);

            BigDecimal balance = checkingAccount.getBalance().subtract(withdrawalRequestDTO.amount());
            checkingAccount.setBalance(balance);
            checkingAccountRepository.save(checkingAccount);
        } finally {
            lock.unlock();
        }
    }
}
