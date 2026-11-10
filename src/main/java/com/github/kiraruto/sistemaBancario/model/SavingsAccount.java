package com.github.kiraruto.sistemaBancario.model;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.dto.SavingsAccountDTO;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

//poupança
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "savings_account")
public class SavingsAccount extends Account {
    @Column(name = "interest_rate")
    private BigDecimal interestRate = BigDecimal.valueOf(0.006743); // Taxa de juros aplicada

    public SavingsAccount(String fullName, LocalDate dateBirth, String address, EnumMaritalStatus maritalStatus,
                          String phoneNumber, String email, String cpf, EnumTypeDocument rgOrCnh,
                          String documentNumber, BigDecimal interestRate, BigDecimal balance, Boolean isActive) {
        super(fullName, dateBirth, address, maritalStatus, phoneNumber, email, cpf, rgOrCnh, documentNumber, isActive);
        this.interestRate = interestRate;
        this.setBalance(balance);
    }

    public SavingsAccount(SavingsAccountDTO savingsAccountDTO) {
        super(savingsAccountDTO);
        this.setBalance(savingsAccountDTO.balance());
    }

    public SavingsAccount(BalanceDTO balance) {
        this.setFullName(balance.fullName());
        this.setBalance(balance.balance());
    }

    public SavingsAccount(UUID savingsAccountId) {
        super(savingsAccountId);
    }

    //    aplicando juros
    public void applyInterest() {
        System.out.println("Aplicando juros da poupança: " + interestRate + "% ao mês.");
    }
}
