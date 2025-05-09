package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WithdrawalValidatorService {

    private final TransactionRepository transactionRespository;

    public WithdrawalValidatorService(TransactionRepository transactionRespository) {
        this.transactionRespository = transactionRespository;
    }

    public void validateWithdrawalLimit(UUID accountId, BigDecimal withdrawalAmount) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        BigDecimal totalWithdrawnLast24Hours = transactionRespository
                .sumWithdrawalsByAccountAndDateTimeRange(accountId,
                        EnumTransactionType.SAQUE,
                        twentyFourHoursAgo,
                        now)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalWithCurrent = totalWithdrawnLast24Hours.add(withdrawalAmount);

        if (totalWithCurrent.compareTo(BigDecimal.valueOf(2000)) > 0) {
            throw new IllegalArgumentException("Limite de saque das últimas 24 horas excedido (R$ 2.000,00)");
        }
    }
}
