package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.exceptions.InvalidAccountIdException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidAmountException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidCpfException;
import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatusAlert;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AlertAMLRepository alertAMLRepository;

    @Mock
    private KafkaTemplate<String, AlertAML> kafkaTemplate;

    @InjectMocks
    private AuditService auditService;

    @Test
    void registerAlertAML() {
        String accountId = "12345";
        BigDecimal amount = new BigDecimal("1000.50");
        String cpf = "12345678900";
        AlertAML expectedAlert = new AlertAML();
        expectedAlert.setAccountId(accountId);
        expectedAlert.setAmount(amount);
        expectedAlert.setCpf(cpf);
        expectedAlert.setDate(LocalDateTime.now());
        expectedAlert.setStatus(EnumStatusAlert.NOVO);

        when(alertAMLRepository.save(any(AlertAML.class))).thenReturn(expectedAlert);

        auditService.registerAlertAML(accountId, amount, cpf);

        verify(alertAMLRepository).save(any(AlertAML.class));
        verify(kafkaTemplate).send(eq("alertas-aml"), any(AlertAML.class));
    }

    @Test
    void registerAlertAML_ShouldSetCorrectStatus() {
        String accountId = String.valueOf(UUID.randomUUID());
        BigDecimal amount = new BigDecimal("500.00");
        String cpf = "98765432100";

        auditService.registerAlertAML(accountId, amount, cpf);

        verify(alertAMLRepository).save(argThat(alert ->
                alert.getStatus() == EnumStatusAlert.NOVO
        ));
    }

    @Test
    void registerAlertAML_ShouldSetCurrentDateTime() {
        String accountId = "12345";
        BigDecimal amount = new BigDecimal("750.25");
        String cpf = "11122233344";
        LocalDateTime beforeTest = LocalDateTime.now();

        auditService.registerAlertAML(accountId, amount, cpf);

        verify(alertAMLRepository).save(argThat(alert ->
                alert.getDate() != null &&
                        !alert.getDate().isBefore(beforeTest) &&
                        !alert.getDate().isAfter(LocalDateTime.now())
        ));
    }

    @Test
    void registerAlertAML_ShouldHandleNullAmount() {
        String accountId = "12345";
        BigDecimal nullAmount = null;
        String cpf = "55566677788";

        assertThrows(InvalidAmountException.class, () -> {
            auditService.registerAlertAML(accountId, nullAmount, cpf);
        });
    }

    @Test
    void registerAlertAML_ShouldHandleEmptyAccountId() {
        String emptyAccountId = "";
        BigDecimal amount = new BigDecimal("300.00");
        String cpf = "26189522009";

        assertThrows(InvalidAccountIdException.class, () -> {
            auditService.registerAlertAML(emptyAccountId, amount, cpf);
        });
    }

    @Test
    void registerAlertAML_ShouldHandleEmptyCpf() {
        String emptyAccountId = "12345";
        BigDecimal amount = new BigDecimal("300.00");
        String cpf = "";

        assertThrows(InvalidCpfException.class, () -> {
            auditService.registerAlertAML(emptyAccountId, amount, cpf);
        });
    }
}