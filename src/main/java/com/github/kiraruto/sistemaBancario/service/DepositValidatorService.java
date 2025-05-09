package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.exceptions.DepositLimitExceededException;
import com.github.kiraruto.sistemaBancario.exceptions.FrequentLargeDepositsException;
import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.utils.TransactionValidate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DepositValidatorService {

    private final TransactionRepository transactionRespository;
    private final AlertAMLRepository alertAMLRepository;
    private final AuditService auditService;
    private final TransactionValidate transactionValidate;

    public DepositValidatorService(TransactionRepository transactionRespository,
                                   AlertAMLRepository alertAMLRepository,
                                   AuditService auditService,
                                   TransactionValidate transactionValidate) {
        this.transactionRespository = transactionRespository;
        this.alertAMLRepository = alertAMLRepository;
        this.auditService = auditService;
        this.transactionValidate = transactionValidate;
    }

    public void validateDepositLimit(CheckingAccount checkingAccount, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            auditService.registerAlertAML(checkingAccount.getId().toString(), amount, checkingAccount.getCpf());
            throw new DepositLimitExceededException("O valor do depósito excede o limite permitido de R$ 50.000");
        }
    }

    public void detectSuspiciousDeposits(UUID accountId, CheckingAccount checkingAccount, WithdrawRequestDTO requestDTO) {
        LocalDateTime dataLimite = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(24);
        List<Transaction> last24hTransactions = transactionRespository.findLasts24Hours(accountId, dataLimite);

        Map<BigDecimal, List<Transaction>> transactionsByAmount = last24hTransactions.stream()
                .filter(t -> t.getTransactionType().equals(EnumTransactionType.DEPOSITO))
                .collect(Collectors.groupingBy(Transaction::getAmount));

        for (Map.Entry<BigDecimal, List<Transaction>> entry : transactionsByAmount.entrySet()) {
            List<Transaction> sameValueTransactions = entry.getValue().stream()
                    .sorted(Comparator.comparing(Transaction::getTransactionDate))
                    .toList();

            for (int i = 0; i <= sameValueTransactions.size() - 3; i++) {
                LocalDateTime first = sameValueTransactions.get(i).getTransactionDate();
                LocalDateTime third = sameValueTransactions.get(i + 2).getTransactionDate();
                Duration interval = Duration.between(first, third);
                if (interval.toMinutes() < 5) {
                    Transaction fraudTransaction = new Transaction(requestDTO, transactionValidate, EnumStatus.PENDENTE);
                    fraudTransaction.setDescription("Depósito suspeito: 3 valores iguais em < 5 minutos");
                    transactionRespository.save(fraudTransaction);

                    alertAMLRepository.save(new AlertAML(fraudTransaction, checkingAccount.getCpf()));
                    throw new IllegalStateException("Fraude detectada: três depósitos do mesmo valor em menos de 5 minutos.");
                }
            }
        }
    }

    public void checkFrequentLargeDeposits(UUID accountId, CheckingAccount checkingAccount, WithdrawRequestDTO requestDTO, BigDecimal amount) {
        LocalDateTime dataLimite = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(24);
        List<Transaction> last24hTransactions = transactionRespository.findLasts24Hours(accountId, dataLimite);

        long count = last24hTransactions.stream()
                .filter(t -> t.getTransactionType().equals(EnumTransactionType.DEPOSITO))
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(9000)) >= 0)
                .count();

        if (amount.compareTo(BigDecimal.valueOf(9000)) >= 0 && count >= 5) {
            Transaction fraudTransaction = new Transaction(requestDTO, transactionValidate, EnumStatus.PENDENTE);
            fraudTransaction.setDescription("Depósito suspeito por recorrência");
            transactionRespository.save(fraudTransaction);

            alertAMLRepository.save(new AlertAML(fraudTransaction, checkingAccount.getCpf()));

            throw new FrequentLargeDepositsException("Depósito bloqueado devido à recorrência de depósitos acima de R$ 9.000 na conta.");
        }
    }

    public void checkDepositAboveTenThousand(CheckingAccount checkingAccount, WithdrawRequestDTO requestDTO, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
            Transaction fraudTransaction = new Transaction(requestDTO, transactionValidate, EnumStatus.PENDENTE);
            fraudTransaction.setDescription("Depósito acima de R$ 10.000 - Verificação manual");
            transactionRespository.save(fraudTransaction);

            alertAMLRepository.save(new AlertAML(fraudTransaction, checkingAccount.getCpf()));

            throw new IllegalStateException("Depósito acima de R$ 10.000 - Verificação manual necessária.");
        }
    }
}
