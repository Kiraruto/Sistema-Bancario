package com.github.kiraruto.sistemaBancario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SavingsAccountDTO(
        @Size(max = 100, message = "O nome não pode ter mais de 100 caracteres")
        @NotBlank(message = "O nome completo não pode ser em branco")
        @JsonAlias({"full_name", "nome_completo"}) // Alias for flexibility in JSON naming
        String fullName,

        @PastOrPresent(message = "A data de nascimento deve ser no passado ou no presente")
        @NotNull(message = "A data de nascimento não pode ser nula")
        @JsonAlias({"date_of_birth", "data_nascimento"}) // Alias for flexibility in JSON naming
        LocalDate dateBirth,

        @Size(max = 200, message = "O endereço não pode ter mais de 200 caracteres")
        @NotBlank(message = "O endereço não pode ser em branco")
        @JsonAlias({"address", "endereco"}) // Alias for flexibility in JSON naming
        String address,

        @NotNull(message = "O estado civil não pode ser nulo")
        @JsonAlias({"marital_status", "estado_civil"}) // Alias for flexibility in JSON naming
        EnumMaritalStatus maritalStatus,

        @Size(max = 11, message = "O número de telefone não pode ter mais de 11 caracteres")
        @NotNull(message = "O número de telefone não pode ser nulo")
        @JsonAlias({"phone_number", "numero_telefone"}) // Alias for flexibility in JSON naming
        String phoneNumber,

        @Email(message = "O e-mail deve ser válido")
        @Size(max = 60, message = "O e-mail não pode ter mais de 60 caracteres")
        @NotBlank(message = "O email não pode ser nulo")
        @JsonAlias({"email", "email_usuario"}) // Alias for flexibility in JSON naming
        String email,

        @CPF(message = "O CPF deve ser válido")
        @Size(max = 11, message = "O CPF não pode ter mais de 11 caracteres")
        @NotNull(message = "O cpf não pode ser nulo")
        @JsonAlias({"cpf", "numero_cpf"}) // Alias for flexibility in JSON naming
        String cpf,

        @NotNull(message = "O tipo de documento não pode ser nulo")
        @JsonAlias({"document_type", "tipo_documento"}) // Alias for flexibility in JSON naming
        EnumTypeDocument rgOrCnh,

        @Size(max = 11, message = "O número do documento não pode ter mais de 11 caracteres")
        @NotBlank(message = "O número do documento não pode ser nulo")
        @JsonAlias({"document_number", "numero_documento"}) // Alias for flexibility in JSON naming
        String documentNumber,

        @NotNull(message = "O saldo da conta não pode ser nulo")
        @JsonAlias({"balance", "saldo"}) // Alias for flexibility in JSON naming
        BigDecimal balance,

        @NotNull(message = "O id do usuario não pode ser nulo")
        @JsonAlias({"user_id", "user"}) // Alias for flexibility in JSON naming
        UUID userId
) {
}
