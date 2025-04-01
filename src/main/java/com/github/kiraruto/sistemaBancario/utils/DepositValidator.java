package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.exceptions.DocLimitExceededException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidDepositOriginException;
import com.github.kiraruto.sistemaBancario.exceptions.ScheduleDocClosedException;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.service.AMLService;
import com.github.kiraruto.sistemaBancario.service.BankHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DepositValidator {

    private final BankHoursService bankHoursService;
    private final AMLService amlService;
    private final SavingsAccountRepository savingsAccountRepository;

    public void validateTed() {
        bankHoursService.isTedTimeValid(LocalDateTime.now());
    }

    public void validateDoc(DepositRequestDTO request) {
        if (request.balance().compareTo(new BigDecimal("4999.99")) > 0) {
            throw new DocLimitExceededException("Limite máximo de DOC excedido.");
        }

        if (LocalDateTime.now().getHour() >= 17) {
            throw new ScheduleDocClosedException("Horário para DOC encerrado.");
        }
    }

    public void validateDeposito(DepositRequestDTO request) {
        if (request.balance().compareTo(new BigDecimal("50000")) > 0) {
            amlService.registerAlert(
                    request.accountSends().toString(),
                    request.balance(),
                    request.cpf(),
                    "DEPOSITO_GRANDE_VALOR"
            );
        }

        if (!List.of("AGENCIA", "ATM").contains(request.origin())) {
            throw new InvalidDepositOriginException("Origem do depósito inválida. Permitido apenas: AGENCIA ou ATM.");
        }
    }

    public void validateDepositoTransiction(WithdrawRequestDTO withdrawRequestDTO) {
        Optional<SavingsAccount> savingsAccount = savingsAccountRepository.findById(withdrawRequestDTO.idAccount());
        var sa = savingsAccount.get();

        if (withdrawRequestDTO.amount().compareTo(new BigDecimal("50000")) > 0) {
            amlService.registerAlert(
                    withdrawRequestDTO.idAccount().toString(),
                    withdrawRequestDTO.amount(),
                    sa.getCpf(),
                    "DEPOSITO_GRANDE_VALOR"
            );
        }

        if (!List.of("AGENCIA", "ATM").contains(withdrawRequestDTO.enumOrigin())) {
            throw new InvalidDepositOriginException("Origem do depósito inválida. Permitido apenas: AGENCIA ou ATM.");
        }
    }

    public void validateDocTransiction(BigDecimal request) {
        if (request.compareTo(new BigDecimal("4999.99")) > 0) {
            throw new DocLimitExceededException("Limite máximo de DOC excedido.");
        }

        if (LocalDateTime.now().getHour() >= 17) {
            throw new ScheduleDocClosedException("Horário para DOC encerrado.");
        }
    }
}
