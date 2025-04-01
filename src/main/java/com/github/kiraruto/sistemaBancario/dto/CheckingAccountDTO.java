package com.github.kiraruto.sistemaBancario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CheckingAccountDTO(@Size(max = 100, message = "O nome não pode ter mais de 100 caracteres")
                                 @NotBlank(message = "O nome completo não pode ser em branco")
                                 String fullName,

                                 @PastOrPresent(message = "A data de nascimento deve ser no passado ou no presente")
                                 @NotNull(message = "A data de nascimento não pode ser nula")
                                 LocalDate dateBirth,

                                 @Size(max = 200, message = "O endereço não pode ter mais de 200 caracteres")
                                 @NotBlank(message = "O endereço não pode ser em branco")
                                 String address,

                                 @NotNull(message = "O estado civil não pode ser nulo")
                                 EnumMaritalStatus maritalStatus,

                                 @Size(max = 11, message = "O número de telefone não pode ter mais de 11 caracteres")
                                 @NotNull(message = "O número de telefone não pode ser nulo")
                                 String phoneNumber,

                                 @Email(message = "O e-mail deve ser válido")
                                 @Size(max = 60, message = "O e-mail não pode ter mais de 60 caracteres")
                                 @NotBlank(message = "O email não pode ser nulo")
                                 String email,

                                 @CPF(message = "O CPF deve ser válido")
                                 @Size(max = 11, message = "O CPF não pode ter mais de 11 caracteres")
                                 @NotNull(message = "O cpf não pode ser nulo")
                                 String cpf,

                                 @NotNull(message = "O tipo de documento não pode ser nulo")
                                 EnumTypeDocument rgOrCnh,

                                 @Size(max = 11, message = "O número do documento não pode ter mais de 11 caracteres")
                                 @NotBlank(message = "O número do documento não pode ser nulo")
                                 String documentNumber,

                                 @NotNull(message = "O saldo da conta não pode ser nulo")
                                 BigDecimal balance,

                                 @NotNull(message = "A opção de cheque especial não pode ser nula")
                                 Boolean hasOverdraft,

                                 @NotNull(message = "A necessidade de comprovação de renda não pode ser nula")
                                 Boolean requiresIncomeProof,

                                 @NotNull(message = "O id do usuario não pode ser nulo")
                                 @JsonAlias("user_id")
                                 UUID userId
) {
}


