package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.RefreshTokenRequest;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.service.AuthenticationService;
import com.github.kiraruto.sistemaBancario.service.JWTService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Candidato: refreshToken retorna null quando o token e invalido.
 *
 * O controller responde 200 com corpo vazio, em vez de 401. O contrato esperado
 * e falhar com AuthenticationException, que o GlobalExceptionHandler ja mapeia
 * para HTTP 401.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bug: refresh token invalido retorna 200 vazio")
class RefreshTokenInvalidTokenBugTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("token invalido deve lancar AuthenticationException (HTTP 401)")
    void invalidTokenShouldThrowAuthenticationException() {
        String token = "token-invalido";
        User user = new User();
        user.setUsername("fulano");

        when(jwtService.extractUserName(token)).thenReturn("fulano");
        when(userRepository.findByUsername("fulano")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(token, user)).thenReturn(false);

        assertThrows(
                AuthenticationException.class,
                () -> authenticationService.refreshToken(new RefreshTokenRequest(token))
        );
    }
}
