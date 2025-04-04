package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.exceptions.InvalidAccountIdException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidAmountException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidCpfException;
import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatusAlert;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AlertAMLRepository alertAMLRepository;
    private final KafkaTemplate<String, AlertAML> kafkaTemplate;

    @Transactional
    public void registerAlertAML(String accountId, BigDecimal amount, String cpf) {
        if (amount == null) {
            throw new InvalidAmountException("Amount cannot be null");
        }
        if (accountId == null || accountId.isEmpty()) {
            throw new InvalidAccountIdException("Account ID cannot be null or empty");
        }
        if (cpf == null || cpf.isEmpty()) {
            throw new InvalidCpfException("CPF cannot be null or empty");
        }

        AlertAML alert = new AlertAML();
        alert.setAccountId(accountId);
        alert.setAmount(amount);
        alert.setCpf(cpf);
        alert.setDate(LocalDateTime.now());
        alert.setStatus(EnumStatusAlert.NOVO);

        alertAMLRepository.save(alert);

        kafkaTemplate.send("alertas-aml", alert);
    }
}
