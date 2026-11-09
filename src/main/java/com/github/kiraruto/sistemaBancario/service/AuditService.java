package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.exceptions.InvalidAccountIdException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidAmountException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidCpfException;
import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatusAlert;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AlertAMLRepository alertAMLRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, AlertAML> kafkaTemplate;

    /**
     * Roda em transação própria para que o alerta sobreviva ao rollback da operação
     * que o originou (depósito bloqueado, fraude detectada etc.).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerAlertAML(String accountId, BigDecimal amount, String cpf) {
        validate(accountId, amount, cpf);

        AlertAML alert = new AlertAML();
        alert.setAccountId(accountId);
        alert.setAmount(amount);
        alert.setCpf(cpf);
        alert.setDate(LocalDateTime.now());
        alert.setStatus(EnumStatusAlert.NOVO);

        alertAMLRepository.save(alert);

        kafkaTemplate.send("alertas-aml", alert);
    }

    /**
     * Registra a transação suspeita e o alerta em uma transação separada, pois o
     * fluxo de depósito lança exceção logo depois e o rollback apagaria a evidência.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFraudAlert(UUID accountId, BigDecimal amount, EnumOrigin origin,
                                   String cpf, String description) {
        validate(accountId == null ? null : accountId.toString(), amount, cpf);

        Transaction fraudTransaction = new Transaction();
        fraudTransaction.setAccountSends(accountId);
        fraudTransaction.setAccountReceive(accountId);
        fraudTransaction.setTransactionType(EnumTransactionType.DEPOSITO);
        fraudTransaction.setAmount(amount);
        fraudTransaction.setTransactionDate(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        fraudTransaction.setStatus(EnumStatus.PENDENTE);
        fraudTransaction.setOrigin(origin);
        fraudTransaction.setDescription(description);

        transactionRepository.save(fraudTransaction);
        alertAMLRepository.save(new AlertAML(fraudTransaction, cpf));
    }

    private void validate(String accountId, BigDecimal amount, String cpf) {
        if (amount == null) {
            throw new InvalidAmountException("Amount cannot be null");
        }
        if (accountId == null || accountId.isEmpty()) {
            throw new InvalidAccountIdException("Account ID cannot be null or empty");
        }
        if (cpf == null || cpf.isEmpty()) {
            throw new InvalidCpfException("CPF cannot be null or empty");
        }
    }
}
