package com.github.kiraruto.sistemaBancario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDTO(@NotBlank(message = "O nome de usuario não poder ser nulo")
                      String username,
                      @NotBlank(message = "A senha não pode ser nula")
                      String password,
                      @NotBlank(message = "O email não pode ser nulo")
                      @Email
                      String email) {
}
