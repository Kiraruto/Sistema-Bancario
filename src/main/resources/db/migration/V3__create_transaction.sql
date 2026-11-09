-- ============================================================
-- V3 - Transações
-- amount agora é DECIMAL(18,2), igual ao BigDecimal da entidade.
-- A coluna account_recive mantém o nome usado pela entidade.
-- ============================================================
CREATE TABLE transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_sends UUID NOT NULL,
    account_recive UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    origem VARCHAR(20),
    description TEXT
);

CREATE INDEX idx_transaction_account_sends ON transaction (account_sends);
CREATE INDEX idx_transaction_account_recive ON transaction (account_recive);
CREATE INDEX idx_transaction_date ON transaction (transaction_date);
