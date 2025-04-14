package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.github.kiraruto.sistemaBancario.utils.VerifyUUID.validateUUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private UUID transactionId;
    private String transactionIdStr;

    @BeforeEach
    void setup() {
        transactionId = UUID.randomUUID();
        transactionIdStr = transactionId.toString();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void shouldCompleteTransactionSuccessfully() throws Exception {
        try (MockedStatic<com.github.kiraruto.sistemaBancario.utils.VerifyUUID> uuidMock = mockStatic(com.github.kiraruto.sistemaBancario.utils.VerifyUUID.class)) {
            uuidMock.when(() -> validateUUID(transactionIdStr)).thenReturn(transactionId);
            doNothing().when(transactionService).completed(transactionId);

            mockMvc.perform(put("/transaction/{id}/complete", transactionIdStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(transactionService).completed(transactionId);
        }
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void shouldFailTransactionSuccessfully() throws Exception {
        try (MockedStatic<com.github.kiraruto.sistemaBancario.utils.VerifyUUID> uuidMock = mockStatic(com.github.kiraruto.sistemaBancario.utils.VerifyUUID.class)) {
            uuidMock.when(() -> validateUUID(transactionIdStr)).thenReturn(transactionId);
            doNothing().when(transactionService).failure(transactionId);

            mockMvc.perform(put("/transaction/{id}/failure", transactionIdStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(transactionService).failure(transactionId);
        }
    }
}
