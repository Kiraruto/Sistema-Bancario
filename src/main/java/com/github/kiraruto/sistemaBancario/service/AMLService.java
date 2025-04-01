package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AMLService {

    private final AlertAMLRepository alertAMLRepository;

    @Transactional
    public void registerAlert(String contaId, BigDecimal valor, String cpfCliente, String motivo) {
        AlertAML alerta = new AlertAML();
        alerta.setAccountId(contaId);
        alerta.setAmount(valor);
        alerta.setCpf(cpfCliente);
        alerta.setDate(LocalDateTime.now());
        alerta.setObservacoes(motivo);

        alertAMLRepository.save(alerta);
    }
}
