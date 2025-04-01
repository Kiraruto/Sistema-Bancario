package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.UserValidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserValidate userValidate;

    public List<User> getAllUser() {
        return userRepository.findAllByIsActiveTrue();
    }

    public User createUserAccount(UserDTO userDTO) {
        userValidate.validateCreateAccountUser(userDTO);

        User user = new User(userDTO);
        userRepository.save(user);

        System.out.println("Usuário salvo com ID: " + user.getId());

        return user;
    }

    public void disableUser(UUID uuid) {
        User user = userValidate.validateUserTrueToFalse(uuid);

        user.setActive(false);
        userRepository.save(user);
    }

    public void activateUser(UUID uuid) {
        User user = userValidate.validateUserFalseToTrue(uuid);

        user.setActive(true);
        userRepository.save(user);
    }

    public User getUserById(UUID uuid) {
        return userRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe!"));
    }

    public User updateUser(UUID uuid, UserDTO userDTO) {
        User user = userValidate.validateUserTrueToFalse(uuid);

        user.setEmail(userDTO.email());
        user.setUsername(userDTO.username());
        user.setPassword(userDTO.password());
        user.setActive(true);

        return userRepository.save(user);
    }

    public void userToAdmin(UUID uuid) {
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe!"));

        if (user.getRole() == EnumUserRole.ADMIN) {
            throw new IllegalArgumentException("O usuário já é ADMIN!");
        }

        user.setRole(EnumUserRole.ADMIN);
        userRepository.save(user);
    }

    public void userToGerente(UUID uuid) {
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe!"));

        if (user.getRole() == EnumUserRole.GERENTE) {
            throw new IllegalArgumentException("O usuário já é GERENTE!");
        }

        user.setRole(EnumUserRole.GERENTE);
        userRepository.save(user);
    }

    public void userToCliente(UUID uuid) {
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe!"));

        if (user.getRole() == EnumUserRole.CLIENTE) {
            throw new IllegalArgumentException("O usuário já é CLIENTE!");
        }

        user.setRole(EnumUserRole.CLIENTE);
        userRepository.save(user);
    }

}
