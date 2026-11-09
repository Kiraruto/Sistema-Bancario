-- ============================================================
-- V8 - Controle de concorrência otimista das contas
-- Cada atualização incrementa a versão. Duas transações que leem a
-- mesma versão não podem gravar ambas: a segunda falha em vez de
-- sobrescrever silenciosamente o saldo.
-- ============================================================
ALTER TABLE savings_account ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE checking_account ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
