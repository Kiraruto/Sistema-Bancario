package com.github.kiraruto.sistemaBancario.model;

import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@MappedSuperclass
public abstract class Account {
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

    /**
     * Controle de concorrência otimista: duas atualizações simultâneas na mesma
     * conta não podem sobrescrever o saldo uma da outra. A segunda transação a
     * gravar recebe uma falha de concorrência em vez de perder a primeira.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Construtor usado pelas subclasses. A versão é gerenciada pelo Hibernate
     * e não deve ser informada na criação da conta.
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
        this.cpf = cpf;
        this.rgOrCnh = rgOrCnh;
        this.documentNumber = documentNumber;
        this.isActive = isActive;
    }
}
