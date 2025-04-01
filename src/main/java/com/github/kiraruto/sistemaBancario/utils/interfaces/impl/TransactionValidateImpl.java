package com.github.kiraruto.sistemaBancario.utils.interfaces.impl;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.exceptions.AccountNotFoundException;
import com.github.kiraruto.sistemaBancario.exceptions.TypeTransactionInvalidException;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.service.AuditService;
import com.github.kiraruto.sistemaBancario.utils.DepositValidator;
import com.github.kiraruto.sistemaBancario.utils.interfaces.TransactionValidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionValidateImpl implements TransactionValidate {

    private final TransactionRepository transactionRespository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AuditService auditService;
    private final DepositValidator depositValidator;

    public EnumStatus deposit(DepositRequestDTO depositRequestDTO) {
        BigDecimal valorDeposito = depositRequestDTO.balance();
        String contaDestinoId = depositRequestDTO.accountReceive().toString();

        SavingsAccount contaDestino = savingsAccountRepository.findById(UUID.fromString(contaDestinoId))
                .orElseThrow(() -> new AccountNotFoundException("A conta com o id: " + contaDestinoId + " não foi encontrada"));

        if (contaDestino.getIsActive()) {
            return EnumStatus.FALHOU;
        }

        if (valorDeposito.compareTo(BigDecimal.valueOf(50000)) > 0) {
            auditService.registerAlertAML(depositRequestDTO.accountSends().toString(), depositRequestDTO.balance(), depositRequestDTO.cpf());
        }

        List<Transaction> ultimasTransacoes = transactionRespository
                .findLast24HoursByAccountSends(UUID.fromString(contaDestinoId));

        long depositosRecentes = ultimasTransacoes.stream()
                .filter(t -> t.getTransactionType() == EnumTransactionType.DEPOSITO)
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(9000)) >= 0)
                .count();

        if (depositosRecentes >= 5) {
            System.out.println(
                    "\u001B[33mALERTA: Possível fraude na conta " + contaDestinoId +
                            ". 5 ou mais depósitos suspeitos nas últimas 24h.\u001B[0m"
            );

            return EnumStatus.PENDENTE;
        }

        return EnumStatus.CONCLUIDA;
    }

    public EnumOrigin originDeposit(DepositRequestDTO depositRequestDTO) {
        EnumOrigin origin = depositRequestDTO.origin();

        switch (origin) {
            case TED -> depositValidator.validateTed();
            case DOC -> depositValidator.validateDoc(depositRequestDTO);
            case DINHEIRO_FISICO -> depositValidator.validateDeposito(depositRequestDTO);
            default -> throw new TypeTransactionInvalidException("Tipo de transação inválido: " + origin);
        }

        return origin;
    }

    public EnumOrigin originDepositTransiction(WithdrawRequestDTO withdrawRequestDTO) {
        EnumOrigin origin = withdrawRequestDTO.enumOrigin();

        switch (origin) {
            case TED -> depositValidator.validateTed();
            case DOC -> depositValidator.validateDocTransiction(withdrawRequestDTO.amount());
            case DINHEIRO_FISICO -> depositValidator.validateDepositoTransiction(withdrawRequestDTO);
            default -> throw new TypeTransactionInvalidException("Tipo de transação inválido: " + origin);
        }
        return origin;
    }

}
