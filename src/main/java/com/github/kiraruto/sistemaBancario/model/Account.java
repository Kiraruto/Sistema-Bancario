package com.github.kiraruto.sistemaBancario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kiraruto.sistemaBancario.dto.AccountHolderDTO;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@MappedSuperclass
public abstract class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Identificador único da conta

    @Size(max = 100, message = "O nome não pode ter mais de 100 caracteres")
    @Column(name = "full_name", nullable = false)
    private String fullName; // Nome completo do titular

    @PastOrPresent
    @Column(name = "date_birth", nullable = false)
    private LocalDate dateBirth; // Data de nascimento do titular

    @Column(name = "address", nullable = false)
    @Size(max = 200, message = "O endereço não pode ter mais de 200 caracteres")
    private String address; // Endereço do titular

    @Column(name = "marital_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EnumMaritalStatus maritalStatus; // Estado civil do titular

    @Size(max = 11, message = "O numero de telefone não pode ter mais de 11 caracteres")
    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber; // Número de telefone do titular

    @Email
    @Column(name = "email", unique = true, nullable = false)
    @Size(max = 60, message = "O email não pode ter mais de 60 caracteres")
    private String email; // E-mail do titular

    @CPF
    @Column(name = "cpf", unique = true, nullable = false)
    @Size(min = 11, max = 11, message = "O CPF não pode ter mais de 11 caracteres")
    private String cpf; // CPF do titular

    @Column(name = "rg_or_cnh", nullable = false)
    @Enumerated(EnumType.STRING)
    private EnumTypeDocument rgOrCnh; // Tipo de documento (RG ou CNH)

    @Column(name = "document_number", unique = true, nullable = false)
    @Size(max = 20, message = "O Numero do documento não pode ter mais de 20 caracteres")
    private String documentNumber; // Número do documento (RG ou CNH)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal balance; // Saldo da conta

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /**
     * Construtor usado pelas subclasses que recebem os dados do titular e o
     * estado da conta.
     */
    protected Account(String fullName, LocalDate dateBirth, String address, EnumMaritalStatus maritalStatus,
                      String phoneNumber, String email, String cpf, EnumTypeDocument rgOrCnh,
                      String documentNumber, Boolean isActive) {
        this.fullName = fullName;
        this.dateBirth = dateBirth;
        this.address = address;
        this.maritalStatus = maritalStatus;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.cpf = normalizeCpf(cpf);
        this.rgOrCnh = rgOrCnh;
        this.documentNumber = documentNumber;
        this.isActive = isActive;
    }

    /**
     * Centraliza a cópia dos dados do titular compartilhados pelos DTOs de
     * conta corrente e poupança. Toda conta nova começa ativa.
     */
    protected Account(AccountHolderDTO accountHolderDTO) {
        this.fullName = accountHolderDTO.fullName();
        this.dateBirth = accountHolderDTO.dateBirth();
        this.address = accountHolderDTO.address();
        this.maritalStatus = accountHolderDTO.maritalStatus();
        this.phoneNumber = accountHolderDTO.phoneNumber();
        this.email = accountHolderDTO.email();
        this.cpf = normalizeCpf(accountHolderDTO.cpf());
        this.rgOrCnh = accountHolderDTO.rgOrCnh();
        this.documentNumber = accountHolderDTO.documentNumber();
        this.isActive = true;
    }

    /**
     * Construtor usado para referenciar uma conta apenas pelo identificador.
     */
    protected Account(UUID id) {
        this.id = id;
    }

    private static String normalizeCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("[^\\d]", "");
    }
}
