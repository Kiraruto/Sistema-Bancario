package com.github.kiraruto.sistemaBancario.model;

import com.github.kiraruto.sistemaBancario.dto.CheckingAccountDTO;
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

//corrente
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "checking_account")
public class CheckingAccount extends Account {
    @Column(name = "has_over_draft", nullable = false)
    private Boolean hasOverdraft; // Indica se a conta tem cheque especial (limite extra)

    @Column(name = "requires_income_proof", nullable = false)
    private Boolean requiresIncomeProof; // Indica se é necessário comprovação de renda para abrir a conta

    public CheckingAccount(String fullName, LocalDate dateBirth, String address, EnumMaritalStatus maritalStatus,
                           String phoneNumber, String email, String cpf, EnumTypeDocument rgOrCnh,
                           String documentNumber, BigDecimal balance, Boolean hasOverdraft, Boolean requiresIncomeProof, Boolean isActive) {
        super(fullName, dateBirth, address, maritalStatus, phoneNumber, email, cpf, rgOrCnh, documentNumber, isActive);
        this.setBalance(balance);
        this.hasOverdraft = hasOverdraft;
        this.requiresIncomeProof = requiresIncomeProof;
    }

    public CheckingAccount(CheckingAccountDTO checkingAccountDTO) {
        super(checkingAccountDTO);
        this.setBalance(checkingAccountDTO.balance());
        this.hasOverdraft = checkingAccountDTO.hasOverdraft();
        this.requiresIncomeProof = checkingAccountDTO.requiresIncomeProof();
    }

    public CheckingAccount(UUID checkingAccountId) {
        super(checkingAccountId);
    }
}
