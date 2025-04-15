package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.exceptions.DepositLimitExceededException;
import com.github.kiraruto.sistemaBancario.exceptions.FrequentLargeDepositsException;
import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.SavingsAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.TransactionValidate;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountValidate savingsAccountValidate;
    private final TransactionRepository transactionRespository;
    private final TransactionValidate transactionValidate;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AlertAMLRepository alertAMLRepository;

    private static final Logger log = LoggerFactory.getLogger(SavingsAccountService.class);

    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(UUID id) {
        return locks.computeIfAbsent(id, k -> new ReentrantLock());
    }

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
        UUID senderId = depositRequestDTO.accountSends();
        UUID receiverId = depositRequestDTO.accountReceive();

        ReentrantLock lock1 = getLock(senderId);
        ReentrantLock lock2 = getLock(receiverId);

        if (senderId.compareTo(receiverId) < 0) {
            lock1.lock();
            lock2.lock();
        } else {
            lock2.lock();
            lock1.lock();
        }

        try {
            savingsAccountValidate.validateDepositSavings(depositRequestDTO);

            SavingsAccount sa1 = savingsAccountRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Conta de origem não existe"));
            SavingsAccount sa2 = savingsAccountRepository.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Conta de destino não existe"));

            if (depositRequestDTO.balance().compareTo(sa1.getBalance()) > 0) {
                throw new IllegalArgumentException("Saldo insuficiente para transferência");
            }

            BigDecimal newBalance1 = sa1.getBalance().subtract(depositRequestDTO.balance());
            BigDecimal newBalance2 = sa2.getBalance().add(depositRequestDTO.balance());

            sa1.setBalance(newBalance1);
            sa2.setBalance(newBalance2);

            savingsAccountRepository.save(sa1);
            savingsAccountRepository.save(sa2);

            Transaction transaction = new Transaction(depositRequestDTO, transactionValidate);
            transaction.setDescription("Transferência entre Contas Poupança");
            transactionRespository.save(transaction);
        } finally {
            lock1.unlock();
            lock2.unlock();
        }
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

    @Transactional
    public void deposit(UUID uuid, WithdrawRequestDTO withdrawRequestDTO) {
        ReentrantLock lock = getLock(uuid);
        lock.lock();
        try {
            savingsAccountValidate.validateSavingsAccountDeposit(withdrawRequestDTO, uuid);

            SavingsAccount savingsAccount = savingsAccountRepository.findById(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("A conta com este id não existe"));

            BigDecimal valorDeposito = withdrawRequestDTO.amount();

            if (valorDeposito.compareTo(BigDecimal.valueOf(50000)) > 0) {
                auditService.registerAlertAML(
                        savingsAccount.getId().toString(),
                        valorDeposito,
                        savingsAccount.getCpf()
                );

                throw new DepositLimitExceededException("O valor do depósito excede o limite permitido de R$ 50.000");
            }

            LocalDateTime dataLimite = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(24);
            List<Transaction> ultimasTransacoes = transactionRespository.findLasts24Hours(uuid, dataLimite);
            log.debug("Transações nas últimas 24h: {}", ultimasTransacoes.size());

            Map<BigDecimal, List<Transaction>> transacoesPorValor = ultimasTransacoes.stream()
                    .filter(t -> t.getTransactionType().equals(EnumTransactionType.DEPOSITO))
                    .collect(Collectors.groupingBy(Transaction::getAmount));

            for (Map.Entry<BigDecimal, List<Transaction>> entry : transacoesPorValor.entrySet()) {
                List<Transaction> transacoesMesmoValor = entry.getValue().stream()
                        .sorted(Comparator.comparing(Transaction::getTransactionDate))
                        .toList();

                for (int i = 0; i <= transacoesMesmoValor.size() - 3; i++) {
                    LocalDateTime primeira = transacoesMesmoValor.get(i).getTransactionDate();
                    LocalDateTime terceira = transacoesMesmoValor.get(i + 2).getTransactionDate();
                    Duration intervalo = Duration.between(primeira, terceira);
                    if (intervalo.toMinutes() < 5) {
                        log.warn("[FRAUDE] Três depósitos do mesmo valor ({}) em menos de 5 minutos", entry.getKey());

                        Transaction fraudTransaction = new Transaction(withdrawRequestDTO, transactionValidate, EnumStatus.PENDENTE);
                        fraudTransaction.setDescription("Depósito suspeito: 3 valores iguais em < 5 minutos");
                        transactionRespository.save(fraudTransaction);

                        alertAMLRepository.save(new AlertAML(fraudTransaction, savingsAccount.getCpf()));
                        return;
                    }
                }
            }

            if (valorDeposito.compareTo(BigDecimal.valueOf(10000)) > 0) {
                Transaction fraudTransaction = new Transaction(withdrawRequestDTO, transactionValidate, EnumStatus.PENDENTE);
                fraudTransaction.setDescription("Depósito acima de R$ 10.000 - Verificação manual");
                transactionRespository.save(fraudTransaction);

                alertAMLRepository.save(new AlertAML(fraudTransaction, savingsAccount.getCpf()));

                throw new DepositLimitExceededException("O valor do depósito excede o limite permitido de R$ 50.000");
            }


            Transaction transaction = new Transaction(withdrawRequestDTO, transactionValidate);
            transaction.setDescription("Depósito");
            transactionRespository.save(transaction);

            BigDecimal novoSaldo = savingsAccount.getBalance().add(valorDeposito);
            savingsAccount.setBalance(novoSaldo);
            savingsAccountRepository.save(savingsAccount);
        } finally {
            lock.unlock();
        }
    }

    public void withdrawal(UUID uuid, @Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        ReentrantLock lock = getLock(uuid);
        lock.lock();
        try {
            SavingsAccount savingsAccount = savingsAccountValidate.validateSavingsAccountWithdrawal(withdrawalRequestDTO);

            if (withdrawalRequestDTO.amount().compareTo(savingsAccount.getBalance()) > 0) {
                throw new IllegalArgumentException("Saldo insuficiente");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twentyFourHoursAgo = now.minusHours(24);

            BigDecimal totalWithdrawnLast24Hours = transactionRespository
                    .sumWithdrawalsByAccountAndDateTimeRange(uuid,
                            EnumTransactionType.SAQUE,
                            twentyFourHoursAgo,
                            now)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalWithCurrent = totalWithdrawnLast24Hours.add(withdrawalRequestDTO.amount());

            if (totalWithCurrent.compareTo(BigDecimal.valueOf(2000)) > 0) {
                throw new IllegalArgumentException("Limite de saque das últimas 24 horas excedido (R$ 2.000,00)");
            }

            Transaction transaction = new Transaction(withdrawalRequestDTO);
            transaction.setTransactionType(EnumTransactionType.SAQUE);
            transaction.setDescription("Saque Conta Poupança");
            transaction.setTransactionDate(now);
            transaction.setAccountReceive(withdrawalRequestDTO.idAccount());
            transaction.setAccountSends(withdrawalRequestDTO.idAccount());
            transactionRespository.save(transaction);

            BigDecimal balance = savingsAccount.getBalance().subtract(withdrawalRequestDTO.amount());
            savingsAccount.setBalance(balance);
            savingsAccountRepository.save(savingsAccount);
        } finally {
            lock.unlock();
        }
    }
}
