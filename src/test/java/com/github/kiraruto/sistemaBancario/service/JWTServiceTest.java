package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do P1: um usuário desativado não pode continuar autenticando com
 * um JWT ainda válido. O token só vale se o usuário estiver habilitado.
 */
@DisplayName("JWTService - validade do token")
class JWTServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdA==";

    private JWTService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);

        user = new User();
        user.setUsername("cliente-jwt");
        user.setPassword("password");
        user.setRole(EnumUserRole.CLIENTE);
        user.setActive(true);
    }

    @Test
    @DisplayName("token de usuário ativo deve ser válido")
    void activeUserTokenShouldBeValid() {
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("token de usuário desativado deve ser inválido")
    void disabledUserTokenShouldBeInvalid() {
        String token = jwtService.generateToken(user);
        user.setActive(false);

        assertFalse(
                jwtService.isTokenValid(token, user),
                "Usuário desativado não pode autenticar com token ainda não expirado"
        );
    }

    @Test
    @DisplayName("token malformado deve lançar JwtException")
    void malformedTokenShouldThrow() {
        assertThrows(JwtException.class, () -> jwtService.extractUserName("token-malformado"));
    }
}
