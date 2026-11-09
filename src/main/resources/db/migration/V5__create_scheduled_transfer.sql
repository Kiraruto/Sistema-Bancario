-- ============================================================
-- V5 - Transferências agendadas
-- ============================================================
CREATE TABLE scheduled_transfer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_account UUID NOT NULL,
    target_account UUID NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    scheduled_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,

    CONSTRAINT fk_scheduled_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_scheduled_user ON scheduled_transfer (user_id);
CREATE INDEX idx_scheduled_status ON scheduled_transfer (status);
CREATE INDEX idx_scheduled_date ON scheduled_transfer (scheduled_date);
