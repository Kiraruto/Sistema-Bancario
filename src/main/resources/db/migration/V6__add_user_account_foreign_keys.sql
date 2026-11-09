-- ============================================================
-- V6 - FKs inversas de users para as contas
-- Só podem ser criadas agora, depois de checking_account e
-- savings_account existirem (resolve a dependência circular).
-- ============================================================
ALTER TABLE users
    ADD CONSTRAINT fk_users_checking_account
        FOREIGN KEY (checking_account) REFERENCES checking_account (id) ON DELETE SET NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_savings_account
        FOREIGN KEY (savings_account) REFERENCES savings_account (id) ON DELETE SET NULL;

ALTER TABLE users
    ADD CONSTRAINT uq_users_checking_account UNIQUE (checking_account);

ALTER TABLE users
    ADD CONSTRAINT uq_users_savings_account UNIQUE (savings_account);
