package com.github.kiraruto.sistemaBancario.model;

import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Candidato: o construtor de AlertAML grava o id da TRANSACAO no campo accountId.
 *
 * Contrato esperado: accountId deve conter o id da CONTA envolvida na transacao,
 * pois os alertas sao consultados/agrupados por conta. Hoje o alerta fica apontando
 * para um id de transacao, tornando o vinculo com a conta impossivel.
 */
@DisplayName("Bug: AlertAML usa id da transacao como accountId")
class AlertAMLAccountIdBugTest {

    @Test
    @DisplayName("accountId deve ser o id da conta, nao o id da transacao")
    void shouldStoreAccountIdFromTransaction() {
        UUID transactionId = UUID.randomUUID();
        UUID accountReceive = UUID.randomUUID();

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccountSends(UUID.randomUUID());
        transaction.setAccountReceive(accountReceive);
        transaction.setAmount(new BigDecimal("15000.00"));

        AlertAML alert = new AlertAML(transaction, "52998224725");

        assertEquals(accountReceive.toString(), alert.getAccountId());
    }
}
