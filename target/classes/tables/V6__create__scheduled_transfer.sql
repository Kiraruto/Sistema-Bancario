CREATE TABLE scheduled_transfer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_account UUID NOT NULL,                    -- Conta de origem
    target_account UUID NOT NULL,                    -- Conta de destino
    amount DECIMAL(15,2) NOT NULL,                   -- Valor da transferência
    scheduled_date TIMESTAMP NOT NULL,               -- Data agendada da transferência
    status VARCHAR(50) NOT NULL,                     -- Status da transferência
    retry_count INT NOT NULL DEFAULT 0,              -- Número de tentativas de execução
    user_id UUID NOT NULL,                           -- ID do usuário (chave estrangeira)
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE  -- Chave estrangeira para a tabela users
);
