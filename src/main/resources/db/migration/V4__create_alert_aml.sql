-- ============================================================
-- V4 - Alertas AML
-- ============================================================
CREATE TABLE alert_aml (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    date TIMESTAMP NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    status VARCHAR(50),
    observacoes VARCHAR(200)
);

CREATE INDEX idx_alert_aml_account_id ON alert_aml (account_id);
