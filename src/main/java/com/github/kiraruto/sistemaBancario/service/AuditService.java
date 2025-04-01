package com.github.kiraruto.sistemaBancario.service;

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
        AlertAML alert = new AlertAML();
        alert.setAccountId(accountId);
        alert.setAmount(amount);
        alert.setCpf(cpf);
        alert.setDate(LocalDateTime.now());
        alert.setStatus(EnumStatusAlert.NOVO);

        alertAMLRepository.save(alert);

        kafkaTemplate.send("alertas-aml", alert);
    }

//    @Scheduled(cron = "0 0 8 * * ?")
//    public void enviarRelatorioCompliance() {
//        List<AlertAML> alertasNovos = alertAMLRepository.findByStatus(EnumStatusAlert.NOVO);
//        // Envia email com alertas pendentes
//    }
}
