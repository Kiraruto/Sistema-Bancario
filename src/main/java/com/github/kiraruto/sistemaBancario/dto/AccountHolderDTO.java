package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;

import java.time.LocalDate;

/**
 * Dados do titular comuns aos DTOs de conta corrente e poupança.
 */
public interface AccountHolderDTO {
    String fullName();

    LocalDate dateBirth();

    String address();

    EnumMaritalStatus maritalStatus();

    String phoneNumber();

    String email();

    String cpf();

    EnumTypeDocument rgOrCnh();

    String documentNumber();
}
