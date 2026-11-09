package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.global.exception.GlobalExceptionHandler;
import com.github.kiraruto.sistemaBancario.service.TransferService;
import com.github.kiraruto.sistemaBancario.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão do P1: o controller precisa aplicar o @Valid do DTO. Antes, o
 * update de usuário aceitava corpo inválido e a transferência chegava nula
 * na regra de negócio.
 */
@DisplayName("Controllers - validação dos DTOs")
class RequestValidationTest {

    private final MockMvc transferMvc = MockMvcBuilders
            .standaloneSetup(new TransferController(mock(TransferService.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private final MockMvc userMvc = MockMvcBuilders
            .standaloneSetup(new UserController(mock(UserService.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("transferência inválida deve responder 422")
    void invalidTransferShouldReturn422() throws Exception {
        transferMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromId": null,
                                  "fromType": null,
                                  "toId": null,
                                  "toType": null,
                                  "amount": -10
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("update de usuário inválido deve responder 422")
    void invalidUserUpdateShouldReturn422() throws Exception {
        userMvc.perform(put("/users/{id}/update", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "firstName": "",
                                  "lastName": "",
                                  "password": "",
                                  "email": "email-invalido"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
