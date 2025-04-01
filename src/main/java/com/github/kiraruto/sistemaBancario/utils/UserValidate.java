package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserValidate {

    private final UserRepository userRepository;

    public void validateCreateAccountUser(UserDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.username())) {
            throw new IllegalArgumentException("Usuário já existe!");
        }

        if (userRepository.existsByEmail(userDTO.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado!");
        }
    }

    public User validateUserTrueToFalse(UUID uuid) {
        var user = userRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe!"));

        if (!Boolean.TRUE.equals(userRepository.isActiveById(uuid))) {
            throw new IllegalArgumentException("O usuário já está inativo.");
        }

        return user;
    }

    public User validateUserFalseToTrue(UUID uuid) {
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe!"));

        if (Boolean.TRUE.equals(userRepository.isActiveById(uuid))) {
            throw new IllegalArgumentException("O usuário já está Ativo.");
        }

        return user;
    }
}
