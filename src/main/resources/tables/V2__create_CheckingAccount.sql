CREATE TABLE IF NOT EXISTS checking_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),                                  -- Identificador único da conta
    full_name VARCHAR(100) NOT NULL,                      -- Nome completo do titular
    date_birth DATE NOT NULL,                             -- Data de nascimento
    address VARCHAR(200) NOT NULL,                        -- Endereço
    marital_status VARCHAR(50) NOT NULL,                  -- Estado civil (enum ou string)
    phone_number VARCHAR(11) UNIQUE NOT NULL,             -- Número de telefone (único)
    email VARCHAR(60) UNIQUE NOT NULL,                    -- E-mail (único, mas já está em users)
    cpf VARCHAR(11) UNIQUE NOT NULL,                      -- CPF (único)
    rg_or_cnh VARCHAR(50) NOT NULL,                -- Tipo de documento (RG ou CNH)
    document_number VARCHAR(11) UNIQUE NOT NULL,          -- Número do documento
    is_active BOOLEAN NOT NULL,                           -- Ver se a conta está ativa
    balance DECIMAL(18, 2) DEFAULT 100.00 NOT NULL,       -- Saldo da conta corrente
    has_over_draft BOOLEAN NOT NULL DEFAULT FALSE,        -- Indica se a conta tem cheque especial
    requires_income_proof BOOLEAN NOT NULL DEFAULT FALSE, -- Indica se precisa comprovação de renda

    user_id UUID UNIQUE NOT NULL,                                  -- Chave estrangeira para users
    FOREIGN KEY (user_id) REFERENCES users(id)            -- Relacionamento com users
);