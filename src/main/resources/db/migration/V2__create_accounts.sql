-- ============================================================
-- V2 - Contas
-- As duas tabelas referenciam users.
-- A FK inversa (users -> contas) é criada na V6.
-- ============================================================
CREATE TABLE savings_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    date_birth DATE NOT NULL,
    address VARCHAR(200) NOT NULL,
    marital_status VARCHAR(50) NOT NULL,
    phone_number VARCHAR(11) NOT NULL,
    email VARCHAR(60) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    rg_or_cnh VARCHAR(50) NOT NULL,
    document_number VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    interest_rate DECIMAL(10, 6) NOT NULL DEFAULT 0.006743,
    balance DECIMAL(18, 2) NOT NULL,
    user_id UUID NOT NULL,

    CONSTRAINT uq_savings_phone UNIQUE (phone_number),
    CONSTRAINT uq_savings_email UNIQUE (email),
    CONSTRAINT uq_savings_cpf UNIQUE (cpf),
    CONSTRAINT uq_savings_document UNIQUE (document_number),
    CONSTRAINT fk_savings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE checking_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    date_birth DATE NOT NULL,
    address VARCHAR(200) NOT NULL,
    marital_status VARCHAR(50) NOT NULL,
    phone_number VARCHAR(11) NOT NULL,
    email VARCHAR(60) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    rg_or_cnh VARCHAR(50) NOT NULL,
    document_number VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    balance DECIMAL(18, 2) NOT NULL,
    has_over_draft BOOLEAN NOT NULL DEFAULT FALSE,
    requires_income_proof BOOLEAN NOT NULL DEFAULT FALSE,
    user_id UUID NOT NULL,

    CONSTRAINT uq_checking_phone UNIQUE (phone_number),
    CONSTRAINT uq_checking_email UNIQUE (email),
    CONSTRAINT uq_checking_cpf UNIQUE (cpf),
    CONSTRAINT uq_checking_document UNIQUE (document_number),
    CONSTRAINT fk_checking_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
