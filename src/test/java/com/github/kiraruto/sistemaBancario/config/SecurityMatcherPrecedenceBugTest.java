package com.github.kiraruto.sistemaBancario.config;
import com.github.kiraruto.sistemaBancario.config.SecurityConfiguration;
import com.github.kiraruto.sistemaBancario.controller.SecurityProbeController;

import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.service.CustomUserDetailsService;
import com.github.kiraruto.sistemaBancario.service.JWTService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Candidato C: matchers de seguranca se sobrepoem e bloqueiam o CLIENTE.
 *
 * Contrato esperado: os arrays GERENTE_CLIENTE_PATHS e ADMIN_GERENTE_CLIENTE_PATHS
 * declaram explicitamente deposito/saque/transferencia e /users/{id}/update como
 * acessiveis ao CLIENTE. Porem, ADMIN_PATHS ("/users/**") e ADMIN_GERENTE_PATHS
 * ("/savings-accounts/**", "/checking-accounts/**") eram avaliados ANTES e "engoliam"
 * essas rotas, exigindo ADMIN/GERENTE. Resultado: CLIENTE recebia 403.
 */
@WebMvcTest(controllers = SecurityProbeController.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles({"test", "security-probe"})
class SecurityMatcherPrecedenceBugTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "cliente", authorities = "CLIENTE")
    void clienteShouldAccessCheckingAccountDeposit() throws Exception {
        mockMvc.perform(get("/checking-accounts/{id}/deposit", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente", authorities = "CLIENTE")
    void clienteShouldAccessSavingsAccountBalance() throws Exception {
        mockMvc.perform(get("/savings-accounts/{id}/balance", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente", authorities = "CLIENTE")
    void clienteShouldUpdateOwnUser() throws Exception {
        mockMvc.perform(put("/users/{id}/update", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
