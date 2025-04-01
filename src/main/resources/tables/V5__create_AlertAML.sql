CREATE TABLE alert_aml (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- Identificador único do alerta
    account_id VARCHAR(255) NOT NULL,               -- Identificador da conta associada ao alerta
    amount DECIMAL(15,2) NOT NULL,                  -- Valor envolvido no alerta
    date TIMESTAMP NOT NULL,                        -- Data e hora do alerta
    cpf VARCHAR(14) NOT NULL,                       -- CPF associado à conta
    status VARCHAR(50),                             -- Status do alerta (ex: PENDENTE, APROVADO)
    observacoes VARCHAR(200)                        -- Observações adicionais sobre o alerta
);
