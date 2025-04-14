package com.github.kiraruto.sistemaBancario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SavingsAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private SavingsAccount testAccount;
    private SavingsAccountDTO validAccountDTO;

    @BeforeEach
    void setUp() {
        // Criar usuário de teste
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRole(EnumUserRole.CLIENTE);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);

        // Criar conta de teste
        testAccount = new SavingsAccount();
        testAccount.setUser(testUser);
        testAccount.setBalance(BigDecimal.valueOf(1000));
        testAccount.setIsActive(true);
        testAccount = savingsAccountRepository.save(testAccount);

        // Configurar DTO válido
        validAccountDTO = new SavingsAccountDTO(
                "Fulano de Tal",
                LocalDate.of(1990, 1, 1),
                "Rua Teste, 123",
                EnumMaritalStatus.SOLTEIRO,
                "11999999999",
                "fulano@example.com",
                "12345678901",
                EnumTypeDocument.RG,
                "12345678",
                BigDecimal.valueOf(500),
                testUser.getId()
        );
    }

    @Test
    @WithMockUser(authorities = "GERENTE")
    void getAllSavingsAccount_ShouldReturnAllAccounts() throws Exception {
        UUID accountId = UUID.randomUUID();

        SavingsAccount mockAccount = new SavingsAccount();
        mockAccount.setId(accountId);
        mockAccount.setFullName("João da Silva");
        mockAccount.setCpf("12345678900");
        mockAccount.setBalance(BigDecimal.valueOf(1000.0));
        mockAccount.setIsActive(true);

        List<SavingsAccount> accounts = List.of(mockAccount);

        when(savingsAccountRepository.findAll()).thenReturn(accounts);

        mockMvc.perform(get("/savings-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(accountId.toString()))
                .andExpect(jsonPath("$[0].accountNumber").value("123456"))
                .andExpect(jsonPath("$[0].balance").value(1000.0))
                .andExpect(jsonPath("$[0].fullName").value("João da Silva"))
                .andExpect(jsonPath("$[0].cpf").value("12345678900"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void getById_WithValidId_ShouldReturnAccount() throws Exception {
        mockMvc.perform(get("/savings-accounts/" + testAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAccount.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void getById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID invalidId = UUID.randomUUID();
        mockMvc.perform(get("/savings-accounts/" + invalidId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void getTransactions_ShouldReturnTransactions() throws Exception {
        mockMvc.perform(get("/savings-accounts/" + testAccount.getId() + "/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void getBalance_ShouldReturnBalance() throws Exception {
        mockMvc.perform(get("/savings-accounts/" + testAccount.getId() + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void createAccountSa_WithValidData_ShouldCreateAccount() throws Exception {
        mockMvc.perform(post("/savings-accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAccountDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void createAccountSa_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        SavingsAccountDTO invalidDTO = new SavingsAccountDTO(
                "", // Nome vazio
                LocalDate.now().plusDays(1), // Data futura
                "",
                null,
                "",
                "invalid-email",
                "invalid-cpf",
                null,
                "",
                BigDecimal.valueOf(-100), // Saldo negativo
                null
        );

        mockMvc.perform(post("/savings-accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void deposit_ShouldIncreaseBalance() throws Exception {
        WithdrawRequestDTO depositDTO = new WithdrawRequestDTO(
                testAccount.getId(),
                BigDecimal.valueOf(100),
                EnumOrigin.DOC
        );

        BigDecimal initialBalance = testAccount.getBalance();

        mockMvc.perform(put("/savings-accounts/" + testAccount.getId() + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositDTO)))
                .andExpect(status().isNoContent());

        SavingsAccount updatedAccount = savingsAccountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(initialBalance.add(depositDTO.amount()), updatedAccount.getBalance());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void transferRequest_ShouldTransferBetweenAccounts() throws Exception {
        // Criar segunda conta
        SavingsAccount secondAccount = new SavingsAccount();
        secondAccount.setUser(testUser);
        secondAccount.setBalance(BigDecimal.valueOf(500));
        secondAccount.setIsActive(true);
        secondAccount = savingsAccountRepository.save(secondAccount);

        DepositRequestDTO transferDTO = new DepositRequestDTO(
                "Fulano de Tal",
                "fulano@example.com",
                "12345678901",
                BigDecimal.valueOf(100),
                EnumOrigin.DINHEIRO_FISICO,
                testAccount.getId(),
                secondAccount.getId(),
                "Transferência teste",
                testUser.getId()
        );

        mockMvc.perform(put("/savings-accounts/transfer/savingsAccount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDTO)))
                .andExpect(status().isNoContent());

        SavingsAccount updatedSource = savingsAccountRepository.findById(testAccount.getId()).orElseThrow();
        SavingsAccount updatedTarget = savingsAccountRepository.findById(secondAccount.getId()).orElseThrow();

        assertEquals(BigDecimal.valueOf(900), updatedSource.getBalance());
        assertEquals(BigDecimal.valueOf(600), updatedTarget.getBalance());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void disableSavingsAccount_ShouldDeactivateAccount() throws Exception {
        mockMvc.perform(put("/savings-accounts/" + testAccount.getId() + "/disableSavingsAccount"))
                .andExpect(status().isNoContent());

        assertFalse(savingsAccountRepository.findById(testAccount.getId()).orElseThrow().getIsActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateSavingsAccount_ShouldActivateAccount() throws Exception {
        // Primeiro desativar a conta
        testAccount.setIsActive(false);
        savingsAccountRepository.save(testAccount);

        mockMvc.perform(put("/savings-accounts/" + testAccount.getId() + "/activateSavingsAccount"))
                .andExpect(status().isNoContent());

        assertTrue(savingsAccountRepository.findById(testAccount.getId()).orElseThrow().getIsActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void withdrawal_ShouldDecreaseBalance() throws Exception {
        WithdrawalRequestDTO withdrawalDTO = new WithdrawalRequestDTO(
                testAccount.getId(),
                BigDecimal.valueOf(100),
                EnumOrigin.DOC);

        BigDecimal initialBalance = testAccount.getBalance();

        mockMvc.perform(put("/savings-accounts/" + testAccount.getId() + "/withdrawal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalDTO)))
                .andExpect(status().isNoContent());

        SavingsAccount updatedAccount = savingsAccountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(initialBalance.subtract(withdrawalDTO.amount()), updatedAccount.getBalance());
    }

    @Test
    @WithMockUser(roles = "USER")
    void accessWithUserRole_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/savings-accounts"))
                .andExpect(status().isForbidden());
    }
}