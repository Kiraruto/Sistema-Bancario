package com.github.kiraruto.sistemaBancario.global.exception;
import com.github.kiraruto.sistemaBancario.global.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Candidato D: GlobalExceptionHandler mapeia classes de excecao erradas.
 *
 * Contrato esperado: credenciais invalidas (Spring Security BadCredentialsException)
 * devem virar HTTP 401 e acesso negado (Spring Security AccessDeniedException) deve
 * virar HTTP 403. O handler atual importa javax.naming.AuthenticationException e
 * java.nio.file.AccessDeniedException, entao as excecoes reais do Spring Security
 * caem no handler generico e retornam HTTP 500.
 */
class GlobalExceptionHandlerSecurityExceptionBugTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidCredentialsShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/probe/invalid-credentials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessDeniedShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/probe/access-denied"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/invalid-credentials")
        void invalidCredentials() {
            throw new BadCredentialsException("Credenciais invalidas");
        }

        @GetMapping("/probe/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("Acesso negado");
        }
    }
}
