package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.exceptions.BankingHoursException;
import com.github.kiraruto.sistemaBancario.exceptions.DocLimitExceededException;
import com.github.kiraruto.sistemaBancario.exceptions.InvalidDepositOriginException;
import com.github.kiraruto.sistemaBancario.exceptions.ScheduleDocClosedException;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.service.AMLService;
import com.github.kiraruto.sistemaBancario.service.BankHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DepositValidator {

    private final BankHoursService bankHoursService;
    private final AMLService amlService;
    private final SavingsAccountRepository savingsAccountRepository;

    public void validateTed() {
        if (!bankHoursService.isTedTimeValid(LocalDateTime.now())) {
            throw new BankingHoursException("TED só pode ser feito em dias úteis e fora de feriados.");
        }
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

        if (request.origin() != EnumOrigin.DINHEIRO_FISICO) {
            throw new InvalidDepositOriginException("Origem do depósito inválida. Permitido apenas: DINHEIRO_FISICO.");
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

        if (withdrawRequestDTO.enumOrigin() != EnumOrigin.DINHEIRO_FISICO) {
            throw new InvalidDepositOriginException("Origem do depósito inválida. Permitido apenas: DINHEIRO_FISICO.");
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
