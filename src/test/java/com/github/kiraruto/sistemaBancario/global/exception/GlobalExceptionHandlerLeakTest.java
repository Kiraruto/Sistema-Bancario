package com.github.kiraruto.sistemaBancario.global.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão do P1: o tratamento global de erros não pode devolver ao cliente
 * mensagens internas (SQL, nomes de constraints, stack traces) e precisa
 * mapear entradas malformadas para 400 em vez de 500.
 */
@DisplayName("GlobalExceptionHandler - não vaza detalhes internos")
class GlobalExceptionHandlerLeakTest {

    private static final String INTERNAL_DETAIL =
            "relation \"contas_secretas\" does not exist; password=senha-do-banco";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("exceção inesperada deve responder 500 sem vazar a mensagem interna")
    void unexpectedExceptionShouldNotLeakDetails() throws Exception {
        mockMvc.perform(get("/probe/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor."))
                .andExpect(content().string(not(containsString(INTERNAL_DETAIL))))
                .andExpect(content().string(not(containsString("contas_secretas"))))
                .andExpect(content().string(not(containsString("senha-do-banco"))));
    }

    @Test
    @DisplayName("IllegalStateException deve virar 400 com a mensagem de negócio")
    void illegalStateShouldBeBadRequestWithBusinessMessage() throws Exception {
        mockMvc.perform(get("/probe/illegal-state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Verificação manual necessária"));
    }

    @Test
    @DisplayName("corpo JSON malformado deve virar 400 genérico, não 500")
    void malformedJsonShouldReturnGenericBadRequest() throws Exception {
        mockMvc.perform(post("/probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"valor\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Requisição inválida."));
    }

    @Test
    @DisplayName("parâmetro de tipo inválido deve virar 400 genérico, não 500")
    void invalidPathParameterShouldReturnGenericBadRequest() throws Exception {
        mockMvc.perform(get("/probe/account/nao-e-um-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Requisição inválida."));
    }

    @Test
    @DisplayName("violação de integridade deve virar 409 sem expor SQL ou constraint")
    void dataIntegrityViolationShouldReturnGenericConflict() throws Exception {
        mockMvc.perform(get("/probe/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Conflito ao processar a requisição."))
                .andExpect(content().string(not(containsString("uk_contas_cpf"))))
                .andExpect(content().string(not(containsString("INSERT INTO"))));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/unexpected")
        void unexpected() {
            throw new RuntimeException(INTERNAL_DETAIL);
        }

        @GetMapping("/probe/illegal-state")
        void illegalState() {
            throw new IllegalStateException("Verificação manual necessária");
        }

        @PostMapping("/probe/body")
        void body(@RequestBody ProbeBody body) {
        }

        @GetMapping("/probe/account/{id}")
        void account(@PathVariable UUID id) {
        }

        @GetMapping("/probe/integrity")
        void integrity() {
            throw new DataIntegrityViolationException(
                    "could not execute statement; constraint uk_contas_cpf; INSERT INTO contas"
            );
        }
    }

    record ProbeBody(String valor) {
    }
}
