package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.ScheduledTransferDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ValidateScheduleTransfer {

    private final SavingsAccountRepository savingsAccountRepository;

    /**
     * Valida o agendamento antes de persistir. As anotações do DTO cobrem os
     * campos nulos/inválidos na borda HTTP, mas a regra também precisa valer
     * para chamadas diretas ao serviço e para a atualização de um agendamento.
     */
    public void validateTransfer(ScheduledTransferDTO dto) {
        if (dto.accountSends() == null || dto.accountReceive() == null) {
            throw new IllegalArgumentException("As contas de origem e destino são obrigatórias");
        }

        if (dto.accountSends().equals(dto.accountReceive())) {
            throw new IllegalArgumentException("A conta de origem e a conta de destino devem ser diferentes");
        }

        SavingsAccount source = savingsAccountRepository.findById(dto.accountSends())
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada"));
        SavingsAccount target = savingsAccountRepository.findById(dto.accountReceive())
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada"));

        if (!Boolean.TRUE.equals(source.getIsActive())) {
            throw new IllegalArgumentException("A conta de origem está inativa");
        }

        if (!Boolean.TRUE.equals(target.getIsActive())) {
            throw new IllegalArgumentException("A conta de destino está inativa");
        }

        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do agendamento deve ser positivo");
        }

        if (dto.scheduledDate() == null || !dto.scheduledDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("A data do agendamento deve ser futura");
        }
    }
}
