package com.github.kiraruto.sistemaBancario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.dto.SavingsAccountDTO;
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

//poupança
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "savings_account")
public class SavingsAccount extends Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Identificador único da conta

    @Column(name = "interest_rate")
    private BigDecimal interestRate = BigDecimal.valueOf(0.006743); // Taxa de juros aplicada

    @Column(name = "balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal balance; // Saldo da conta poupança

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public SavingsAccount(String fullName, LocalDate dateBirth, String address, EnumMaritalStatus maritalStatus,
                          String phoneNumber, String email, String cpf, EnumTypeDocument rgOrCnh,
                          String documentNumber, BigDecimal interestRate, BigDecimal balance, Boolean isActive) {
        super(fullName, dateBirth, address, maritalStatus, phoneNumber, email, cpf, rgOrCnh, documentNumber, isActive);
        this.interestRate = interestRate;
        this.balance = balance;
    }

    public SavingsAccount(SavingsAccountDTO savingsAccountDTO) {
        this.setFullName(savingsAccountDTO.fullName());
        this.setDateBirth(savingsAccountDTO.dateBirth());
        this.setAddress(savingsAccountDTO.address());
        this.setMaritalStatus(savingsAccountDTO.maritalStatus());
        this.setPhoneNumber(savingsAccountDTO.phoneNumber());
        this.setEmail(savingsAccountDTO.email());
        this.setCpf(new BCryptPasswordEncoder().encode(savingsAccountDTO.cpf()));
        this.setRgOrCnh(savingsAccountDTO.rgOrCnh());
        this.setDocumentNumber(new BCryptPasswordEncoder().encode(savingsAccountDTO.documentNumber()));
        this.setIsActive(true);
        this.balance = savingsAccountDTO.balance();
    }

    public SavingsAccount(BalanceDTO balance) {
        this.setFullName(balance.fullName());
        this.setBalance(balance.balance());
    }

    //    aplicando juros
    public void applyInterest() {
        System.out.println("Aplicando juros da poupança: " + interestRate + "% ao mês.");
    }
}
