package com.github.kiraruto.sistemaBancario.model;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawalRequestDTO;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.utils.interfaces.TransactionValidate;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Transaction {

    @Column(name = "origem")
    public EnumOrigin origin;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    @Column(name = "account_sends", nullable = false)
    private UUID accountSends;
    @Column(name = "account_recive", nullable = false)
    private UUID accountReceive;
    @Column(name = "transaction_type", nullable = false)
    private EnumTransactionType transactionType;
    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;
    @CreatedDate
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
    @Column(name = "status", nullable = false)
    private EnumStatus status;
    @Column(name = "description")
    private String description;

    public Transaction(DepositRequestDTO depositRequestDTO, TransactionValidate transactionValidate) {
        this.accountSends = depositRequestDTO.accountSends();
        this.accountReceive = depositRequestDTO.accountReceive();
        this.transactionType = EnumTransactionType.DEPOSITO;
        this.amount = depositRequestDTO.balance();
        this.transactionDate = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        this.status = transactionValidate.deposit(depositRequestDTO);
        this.origin = transactionValidate.originDeposit(depositRequestDTO);
        this.description = depositRequestDTO.description();
    }

    public Transaction(WithdrawRequestDTO withdrawRequestDTO, TransactionValidate transactionValidate) {
        this.amount = withdrawRequestDTO.amount();
        this.origin = transactionValidate.originDepositTransiction(withdrawRequestDTO);
        this.transactionDate = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        this.transactionType = EnumTransactionType.DEPOSITO;
        this.status = withdrawRequestDTO.amount().compareTo(BigDecimal.valueOf(20000)) > 0
                ? EnumStatus.PENDENTE
                : EnumStatus.CONCLUIDA;
        this.description = "Deposito Conta Pounpança";
    }

    public Transaction(SavingsAccount sourceAccount, SavingsAccount targetAccount, ScheduledTransfer transfer) {
        this.accountSends = sourceAccount.getId();
        this.accountReceive = targetAccount.getId();
        this.transactionType = EnumTransactionType.TRANSFERENCIA;
        this.amount = transfer.getAmount();
        this.transactionDate = transfer.getScheduledDate();
        this.status = EnumStatus.CONCLUIDA;
        this.description = "Transferencia agendada";
    }

    public Transaction(@Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        this.amount = withdrawalRequestDTO.amount();
        this.origin = EnumOrigin.SAQUE;
        this.transactionDate = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        this.transactionType = EnumTransactionType.SAQUE;
        this.status = withdrawalRequestDTO.amount().compareTo(BigDecimal.valueOf(20000)) > 0
                ? EnumStatus.PENDENTE
                : EnumStatus.CONCLUIDA;
        this.description = "Saque Conta Pounpança";
    }
}
