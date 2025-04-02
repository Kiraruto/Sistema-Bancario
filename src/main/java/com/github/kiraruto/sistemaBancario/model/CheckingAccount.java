package com.github.kiraruto.sistemaBancario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kiraruto.sistemaBancario.dto.CheckingAccountDTO;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

//corrente
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "checking_account")
public class CheckingAccount extends Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Identificador único da conta

    @Column(name = "balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal balance; // Saldo da conta corrente

    @Column(name = "has_over_draft", nullable = false)
    private Boolean hasOverdraft; // Indica se a conta tem cheque especial (limite extra)

    @Column(name = "requires_income_proof", nullable = false)
    private Boolean requiresIncomeProof; // Indica se é necessário comprovação de renda para abrir a conta

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public CheckingAccount(String fullName, LocalDate dateBirth, String address, EnumMaritalStatus maritalStatus,
                           String phoneNumber, String email, String cpf, EnumTypeDocument rgOrCnh,
                           String documentNumber, BigDecimal balance, Boolean hasOverdraft, Boolean requiresIncomeProof, Boolean isActive) {
        super(fullName, dateBirth, address, maritalStatus, phoneNumber, email, cpf, rgOrCnh, documentNumber, isActive);
        this.balance = balance;
        this.hasOverdraft = hasOverdraft;
        this.requiresIncomeProof = requiresIncomeProof;
    }

    public CheckingAccount(CheckingAccountDTO checkingAccountDTO) {
        this.setFullName(checkingAccountDTO.fullName());
        this.setDateBirth(checkingAccountDTO.dateBirth());
        this.setAddress(checkingAccountDTO.address());
        this.setMaritalStatus(checkingAccountDTO.maritalStatus());
        this.setPhoneNumber(checkingAccountDTO.phoneNumber());
        this.setEmail(checkingAccountDTO.email());
        this.setCpf(new BCryptPasswordEncoder().encode(checkingAccountDTO.cpf()));
        this.setRgOrCnh(checkingAccountDTO.rgOrCnh());
        this.setDocumentNumber(new BCryptPasswordEncoder().encode(checkingAccountDTO.documentNumber()));
        this.setIsActive(true);
        this.balance = checkingAccountDTO.balance();
        this.hasOverdraft = checkingAccountDTO.hasOverdraft();
        this.requiresIncomeProof = checkingAccountDTO.requiresIncomeProof();
    }
}
