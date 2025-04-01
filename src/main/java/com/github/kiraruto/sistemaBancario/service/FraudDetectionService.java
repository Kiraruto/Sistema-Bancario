package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.exceptions.FraudDetectedException;
import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import com.github.kiraruto.sistemaBancario.repository.TransactionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {
    private final TransactionScheduleRepository transactionRepository;
    private final AMLService amlService;

    public void checkForFraudPatterns(UUID accountId, BigDecimal amount, LocalDateTime timestamp) {
        List<ScheduledTransfer> recentTransactions = transactionRepository
                .findLastHourTransactions(accountId, timestamp.minusMinutes(5));

        if (recentTransactions.size() >= 3) {
            amlService.registerAlert(
                    accountId.toString(),
                    amount,
                    "",
                    "Padrão suspeito: 3 transações em 5 minutos"
            );
            throw new FraudDetectedException("Possível fraude detectada");
        }

        if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
            amlService.registerAlert(
                    accountId.toString(),
                    amount,
                    "",
                    "Transação de alto valor"
            );
        }
    }
}