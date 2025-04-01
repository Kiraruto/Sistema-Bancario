CREATE TABLE transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- Identificador único da transação
    account_sends UUID,                             -- Conta que envia o valor
    account_recive UUID,                            -- Conta que recebe o valor
    transaction_type VARCHAR(50),                   -- Tipo de transação (ex: depósito, transferência)
    amount BIGINT,                                  -- Valor da transação
    transaction_date TIMESTAMP,                     -- Data e hora da transação
    status VARCHAR(50),                             -- Status da transação (ex: concluída, pendente)
    origem VARCHAR(20),                             -- Origem da transação (ex: PIX, TED, DOC)
    description TEXT                                -- Descrição opcional da transação
);
