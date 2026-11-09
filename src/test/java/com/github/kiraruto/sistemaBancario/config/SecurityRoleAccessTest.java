package com.github.kiraruto.sistemaBancario.config;

import com.github.kiraruto.sistemaBancario.controller.SecurityProbeController;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.service.CustomUserDetailsService;
import com.github.kiraruto.sistemaBancario.service.JWTService;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão do P0: ADMIN e GERENTE precisam acessar as rotas de conta.
 *
 * Os matchers amplos de GERENTE/ADMIN eram avaliados depois dos matchers
 * específicos de CLIENTE e acabavam bloqueando os perfis de staff.
 */
@WebMvcTest(controllers = SecurityProbeController.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles({"test", "security-probe"})
@DisplayName("Segurança - acesso por perfil")
class SecurityRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "admin", authorities = "ADMIN")
    @DisplayName("ADMIN deve listar, ler e operar contas")
    void adminShouldAccessAccounts() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(get("/savings-accounts")).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts")).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}/transactions", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}/balance", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}/deposit", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}/withdrawal", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/transfer")).andExpect(status().isOk());
        mockMvc.perform(get("/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gerente", authorities = "GERENTE")
    @DisplayName("GERENTE deve listar, ler e operar contas")
    void gerenteShouldAccessAccounts() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(get("/savings-accounts")).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts")).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}/transactions", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}/balance", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}/deposit", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}/withdrawal", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/transfer")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gerente", authorities = "GERENTE")
    @DisplayName("GERENTE não deve acessar rotas exclusivas de ADMIN")
    void gerenteShouldNotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cliente", authorities = "CLIENTE")
    @DisplayName("CLIENTE deve ler e operar as próprias contas")
    void clienteShouldAccessOwnAccounts() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(get("/savings-accounts/{id}", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}/transactions", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}/balance", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}/balance", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/checking-accounts/{id}/deposit", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/savings-accounts/{id}/withdrawal", accountId)).andExpect(status().isOk());
        mockMvc.perform(get("/transfer")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente", authorities = "CLIENTE")
    @DisplayName("CLIENTE não deve acessar rotas de staff")
    void clienteShouldNotAccessStaffRoutes() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(get("/savings-accounts")).andExpect(status().isForbidden());
        mockMvc.perform(get("/checking-accounts")).andExpect(status().isForbidden());
        mockMvc.perform(get("/savings-accounts/{id}/disableSavingsAccount", accountId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/users")).andExpect(status().isForbidden());
    }
}
