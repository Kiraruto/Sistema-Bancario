package com.github.kiraruto.sistemaBancario.service;
import com.github.kiraruto.sistemaBancario.service.UserService;

import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.UserValidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Candidato B: updateUser grava a senha em texto puro.
 *
 * Contrato esperado: createUserAccount usa passwordEncoder.encode(...) e o login
 * compara com BCrypt. O update deve manter o mesmo contrato (senha sempre com hash).
 * O codigo atual faz user.setPassword(userDTO.password()) sem encode.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceUpdatePasswordBugTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserValidate userValidate;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userValidate, passwordEncoder);
    }

    @Test
    void updateShouldStoreEncodedPassword() {
        UUID userId = UUID.randomUUID();

        User existing = new User();
        existing.setId(userId);
        existing.setUsername("joao");
        existing.setEmail("joao@email.com");
        existing.setPassword("hashAntigo");

        UserDTO dto = new UserDTO(
                "joao",
                "Joao",
                "Silva",
                "novaSenha",
                "novo@email.com"
        );

        when(userValidate.validateUserTrueToFalse(userId)).thenReturn(existing);
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode("novaSenha")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(userId, dto);

        assertEquals(
                "ENCODED",
                result.getPassword(),
                "A senha atualizada deve ser armazenada com hash, como no createUserAccount"
        );
    }
}
