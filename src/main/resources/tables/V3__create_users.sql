CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),                         -- Identificador único do usuário
    username VARCHAR(50) UNIQUE NOT NULL,        -- Nome de usuário (único)
    password VARCHAR(255) NOT NULL,              -- Senha do usuário
    email VARCHAR(100) UNIQUE NOT NULL,          -- E-mail do usuário (único)
    role VARCHAR(8),                             -- Papel do usuário (enum)
    is_active BOOLEAN DEFAULT TRUE,              -- Status do usuário
    checking_account UUID,                       -- Conta corrente
    savings_account UUID,                        -- Conta poupança
    FOREIGN KEY (checking_account) REFERENCES checking_account(id) ON DELETE SET NULL,  -- Opção de comportamento quando a conta for deletada
    FOREIGN KEY (savings_account) REFERENCES savings_account(id) ON DELETE SET NULL,   -- Opção de comportamento quando a conta for deletada
    FOREIGN KEY (id) REFERENCES scheduled_transfer(user_id) ON DELETE CASCADE  -- Chave estrangeira para scheduled_transfer
);
