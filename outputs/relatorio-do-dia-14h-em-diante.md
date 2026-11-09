# Relatório do dia — 09/11/2026, 14:00 → 17:27

**Projeto:** Sistema Bancário (Spring Boot)
**Branch atual:** `1.6.5`
**HEAD:** `8dbc82e`
**Estado da árvore:** limpa
**Resultado final:** 162 testes, 0 falhas, `BUILD SUCCESS`

Este relatório cobre todos os commits feitos hoje a partir das 14h. Não houve
commits entre 14:00 e 15:08.

---

## 1. Resumo executivo

| Métrica | Valor |
|---|---|
| Commits no período | 21 |
| Arquivos alterados | 71 |
| Linhas | +3.150 / −555 |
| Branches trabalhadas | `main`, `1.5.0`, `1.6.0`, `1.6.5` |
| Correções P0 | 5 |
| Correções P1 | 6 |
| Testes ao final | 162 |
| Regressões | 0 |

Evolução da suíte:

| Momento | Testes |
|---|---|
| Linha de base | 113 |
| Após P0 (`1.6.0`) | 131 |
| Após P1 (`1.6.5`) | 162 |

---

## 2. Linha do tempo

### 2.1. 15:08–16:19 — Documentação, infraestrutura e base da 1.5.0

#### `main` (já enviado ao origin)

| Hora | Commit | O que mudou |
|---|---|---|
| 15:08 | `ae33e07` | README reescrito com documentação completa da API |
| 15:18 | `7ee6082` | Remoção de senhas de exemplo do README |

#### Branch `1.5.0` — 6 commits ainda não enviados

| Hora | Commit | O que mudou |
|---|---|---|
| 16:15 | `8e96115` | Admin criado via variáveis de ambiente, sem senha fixa |
| 16:16 | `1a80e80` | Validação de ownership de contas (`AccountAccessService`) |
| 16:16 | `ac6d3cb` | Migração do schema para Flyway (`V1` a `V7`) |
| 16:16 | `6a06fff` | Testcontainers com PostgreSQL 16.3 real |
| 16:19 | `d5d02bf` | Workflow de CI (`.github/workflows/ci.yml`) |
| 16:19 | `dd8393e` | README atualizado para admin, Flyway, Testcontainers e CI |

### 2.2. 16:45–17:09 — Correções P0 na `1.6.0`

| Hora | Commit | Correção | Teste de regressão |
|---|---|---|---|
| 16:45 | `ceebc24` | Conclusão de transação idempotente; impede crédito duplicado | `TransactionServiceIdempotencyTest` |
| 16:50 | `20f24d5` | Transferência atômica com lock pessimista e registro de `TRANSFERENCIA` | `TransferServiceTransactionTest` |
| 16:54 | `d089d03` | Alertas AML/fraude em `REQUIRES_NEW`, sobrevivem ao rollback | `FraudAlertPersistenceTest`, `AuditServiceTest` |
| 16:56 | `e45b28a` | Reordena matchers: ADMIN/GERENTE acessam contas | `SecurityRoleAccessTest`, `SecurityProbeController` |
| 17:00 | `9f3fb9d` | Execução atômica de agendamentos em transação própria | `ScheduledTransferExecutionTest` |
| 17:09 | `344574d` | Isola contexto de teste e remove dependência de horário | Ajustes em `AbstractIntegrationTest` e testes |

**Resultado da `1.6.0`:** 131 testes, 0 falhas.

### 2.3. 17:11–17:27 — Endurecimento P1 na `1.6.5`

| Hora | Commit | Correção | Teste de regressão |
|---|---|---|---|
| 17:11 | `cd4a04c` | JWT malformado não gera 500; usuário desativado não autentica | `JWTServiceTest`, `JWTAuthenticationFilterTest` |
| 17:13 | `3e933ac` | Validação de DTOs de movimentação, agendamento e update de usuário | `DtoValidationTest`, `RequestValidationTest` |
| 17:16 | `1e49740` | Handler global não vaza mensagens internas; 400/409 corretos | `GlobalExceptionHandlerLeakTest` |
| 17:19 | `1bcacb7` | `@Version` nas contas + migration `V8` (concorrência otimista) | `AccountOptimisticLockingTest` |
| 17:22 | `9bc218e` | Extrato de conta inexistente retorna 404 | Teste novo em `SavingsAccountControllerTest` + ajuste em `SavingsAccountServiceTest` |
| 17:26 | `69a6a94` | Validação de agendamento na criação/atualização | `ScheduledTransferValidationTest` (8 testes) |
| 17:27 | `8dbc82e` | README com decisões e limitações da 1.6.5 | — |

**Resultado da `1.6.5`:** 162 testes, 0 falhas.

---

## 3. Detalhamento das correções P0

### 3.1. Conclusão idempotente de transação (`ceebc24`)

- Adicionado lock pessimista nos repositórios de conta.
- `TransactionService` só conclui uma transação `PENDENTE` uma vez.
- Chamadas concorrentes não creditam o saldo duas vezes.
- Regressão: `TransactionServiceIdempotencyTest`.

### 3.2. Transferência atômica (`20f24d5`)

- Débito, crédito e registro da transação na mesma transação de banco.
- Locks adquiridos em ordem determinística para evitar deadlock.
- Registro explícito de `TRANSFERENCIA`.
- Regressão: `TransferServiceTransactionTest`.

### 3.3. Alertas fora do rollback (`d089d03`)

- `AuditService` grava alertas com `REQUIRES_NEW`.
- Alerta AML/fraude não se perde quando a operação principal falha.
- Regressão: `FraudAlertPersistenceTest` e `AuditServiceTest`.

### 3.4. Precedência de matchers (`e45b28a`)

- Rotas literais avaliadas antes dos padrões `/{id}`.
- ADMIN e GERENTE voltam a acessar as rotas de conta.
- Regressão: `SecurityRoleAccessTest` e `SecurityProbeController`.

### 3.5. Agendamentos atômicos (`9f3fb9d`)

- Novo `ScheduledTransferExecutor` com `REQUIRES_NEW`.
- Débito, crédito, transação e status ficam atômicos.
- Se o crédito falha, o débito é desfeito e o agendamento fica `FALHOU`.
- Regressão: `ScheduledTransferExecutionTest` (4 testes).

---

## 4. Detalhamento das correções P1

### 4.1. JWT (`cd4a04c`)

- Token malformado ou com assinatura inválida não gera mais 500.
- O filtro segue sem autenticar e o Spring Security responde 401.
- `isTokenValid` exige `userDetails.isEnabled()`.
- Regressão: 3 testes em `JWTServiceTest` e 4 em `JWTAuthenticationFilterTest`.

### 4.2. Validação de DTOs (`3e933ac`)

- `WithdrawRequestDTO`, `WithdrawalRequestDTO`, `TransferRequestDTO` e
  `ScheduledTransferDTO` com campos obrigatórios e valores positivos.
- `UserController.updateUser` passou a usar `@Valid`.
- Regressão: 7 testes em `DtoValidationTest` e 2 em `RequestValidationTest`.

### 4.3. Tratamento de erros (`1e49740`)

- Erro inesperado retorna 500 genérico; detalhe fica no log.
- Corpo/parâmetro malformado retorna 400 genérico.
- Violação de integridade retorna 409 sem expor SQL ou constraints.
- `IllegalStateException` tratada como erro de negócio (400).
- Regressão: 5 testes em `GlobalExceptionHandlerLeakTest`.

### 4.4. Concorrência otimista (`1bcacb7`)

- `@Version` na base `Account`.
- Migration `V8__add_account_version.sql` adiciona `version` nas duas tabelas.
- Atualização sobre versão desatualizada falha em vez de sobrescrever o saldo.
- Regressão: `AccountOptimisticLockingTest`.

### 4.5. Extrato com 404 (`9bc218e`)

- `getTransactions` lança `EntityNotFoundException` quando a conta não existe.
- O endpoint responde 404 em vez de 400.
- Evidência vermelho→verde: o teste novo falhava com `400` antes da correção.
- Regressão em `SavingsAccountControllerTest` e ajuste em `SavingsAccountServiceTest`.

### 4.6. Validação de agendamento (`69a6a94`)

- `ValidateScheduleTransfer` passou a validar:
  - contas de origem e destino existentes;
  - contas ativas;
  - origem diferente do destino;
  - valor positivo;
  - data futura.
- Validação chamada na criação e na atualização.
- Regressão: `ScheduledTransferValidationTest` (8 testes).

---

## 5. README (`8dbc82e`)

O README foi ajustado e está em `README.md` com 1.010 linhas.

Principais mudanças:

- badge de versão: **1.5.0 → 1.6.5**;
- badge de testes: **113 → 162 passando**;
- checkout do projeto: **`1.6.5`**;
- migration **`V8`** adicionada à tabela;
- regras de agendamento: contas existentes/ativas, origem ≠ destino, valor
  positivo e data futura;
- regras explícitas nos DTOs de depósito, saque e agendamento;
- tabela de erros revisada (400, 401, 404, 409, 500 genérico);
- nova seção **“Decisões de endurecimento (1.6.x)”**;
- limitações novas: rotação de refresh token, `@Data` nas entidades, paginação,
  `System.out` e teste JUnit 4 sem vintage engine.

---

## 6. Situação das branches

| Branch | HEAD | Situação |
|---|---|---|
| `main` | `7ee6082` | Sincronizada com `origin/main` |
| `1.5.0` | `dd8393e` | 6 commits à frente do `origin/1.5.0` |
| `1.6.0` | `344574d` | Local, não enviada |
| `1.6.5` | `8dbc82e` | Local, não enviada; branch atual |

Nenhuma das branches `1.5.0` (commits novos), `1.6.0` ou `1.6.5` foi enviada ao
`origin`.

---

## 7. Validação final

Comando executado na árvore já commitada:

```bash
./mvnw -B -ntp verify
```

Resultado:

```text
Tests run: 162, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Árvore de trabalho limpa ao final.

---

## 8. Roadmap registrado

Itens que ficaram fora do escopo desta rodada:

1. Rotação e revogação de refresh token.
2. Trocar `@Data` das entidades por `equals`/`hashCode` baseados em `id`.
3. Paginação nas listagens.
4. Remover `System.out.println` e padronizar logs.
5. Migrar o teste JUnit 4 (`JobExecutionTest`) para JUnit 5 ou adicionar
   `junit-vintage-engine`.
6. Atualizar Surefire e o compiler plugin.
7. Exercitar Kafka de ponta a ponta nos testes.
