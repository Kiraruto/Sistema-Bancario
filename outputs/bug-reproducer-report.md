# Relatório de Auditoria de Bugs — Sistema Bancário

Data: 2026-11-09  
Projeto: `Sistema-bancario` (Spring Boot 3.4.3)  
Método: descoberta somente leitura → reprodutor focado → aprovação → fix mínimo → red→green → suíte completa.

---

## 1. Resumo executivo

| Métrica | Valor |
|---|---|
| Bugs de lógica provados e corrigidos | **17** (9 nas fases 1–2 + 8 no novo ciclo) |
| Correções de contrato descobertas ao ativar o H2 | **5** |
| Correção de build (MapStruct + Lombok) | **1** |
| Reprodutores do novo ciclo | **14 testes / 9 classes** |
| Suíte completa final | **112 testes, 0 falhas, 0 erros, 0 ignorados** |
| Regressões introduzidas | **0** |
| Classes de teste organizadas por pacote | **28** (+1 controller de sonda) espelhando `src/main` |
| Candidato investigado e rejeitado | **1** (`Transaction` classificando saque como depósito) |

**Estado final:** `./mvnw clean test` → `Tests run: 112, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`.

---

## 2. Infraestrutura de teste (H2)

Antes deste ciclo, 31 testes falhavam porque a suíte tentava usar o PostgreSQL de produção em `localhost:5555`.

### O que foi feito

- Dependência `com.h2database:h2` adicionada com escopo **test**.
- `src/test/resources/application.properties` ativa o profile `test`.
- `src/test/resources/application-test.yml` configura H2 em memória e o schema do Spring Batch:
  - URL `jdbc:h2:mem:sistema_bancario_test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=TRANSACTION`
  - schema `classpath:org/springframework/batch/core/schema-h2.sql`
  - prefixo `BATCH_`, `spring.batch.job.enabled=false`
- A configuração de produção (`application.yml`, PostgreSQL/Kafka/JWT) **não foi alterada**.

### Bloqueios resolvidos

| Bloqueio | Causa | Solução |
|---|---|---|
| `Unknown data type: TINYINT` | `MODE=PostgreSQL` do H2 rejeita `TINYINT` | Removido o `MODE=PostgreSQL` |
| `Ambiguous mapping` | Controller de sonda de segurança duplicava rotas reais | Controller isolado com `@Profile("security-probe")` |

### Correção de build que afetava a aplicação

Os mappers MapStruct eram gerados **vazios** (sem nenhum `set`), porque o MapStruct executava antes do Lombok gerar getters/setters. Isso quebrava criação de contas e transferências agendadas.

- Adicionado `lombok-mapstruct-binding:0.2.0` aos `annotationProcessorPaths` do `pom.xml`.
- Evidência: `SavingsAccountMapperImpl` passou a gerar `setFullName`, `setBalance`, etc.

### Organização dos testes

As classes de teste foram organizadas espelhando os pacotes do código principal:

```
src/test/java/com/github/kiraruto/sistemaBancario/
├── model/
├── service/
├── utils/
├── controller/
├── config/
├── global/exception/
├── batch/config/
└── SistemaBancarioApplicationTests.java
```

- 28 classes de teste distribuídas nos pacotes correspondentes.
- `SecurityProbeController` (apoio, não é teste) em `controller/`.
- `package` e imports atualizados após a mudança.
- Suíte reexecutada verde após a reorganização.

---

## 3. Fases 1 e 2 — bugs já corrigidos

### Fase 1

| # | Bug | Arquivo do fix | Teste |
|---|---|---|---|
| 1 | Depósito/transferência rejeitava qualquer valor positivo (`>= 0` invertido) | `utils/CheckingAccountValidate.java` | `CheckingAccountValidateTransferPositiveBugTest` |
| 2 | Depósito/saque em conta existente era rejeitado como “não existe” (negação ausente) | `utils/CheckingAccountValidate.java` | `CheckingAccountValidateExistingAccountBugTest` |
| 3 | `TransferService` aceitava valor negativo e invertia o fluxo do dinheiro | `service/TransferService.java` | `TransferServiceNegativeAmountBugTest` |
| 4 | Usuário desativado continuava autenticando (`isEnabled()` fixo em `true`) | `model/User.java` | `UserDisabledAuthenticationBugTest` |
| 5 | `updateUser` gravava senha em texto puro | `service/UserService.java` | `UserServiceUpdatePasswordBugTest` |

### Fase 2

| # | Bug | Arquivo do fix | Teste |
|---|---|---|---|
| 6 | Matchers de segurança se sobrepunham e retornavam 403 para o CLIENTE | `config/SecurityConfiguration.java` | `SecurityMatcherPrecedenceBugTest` (3) |
| 7 | Handler mapeava exceções erradas; erros reais viravam 500 | `global/exception/GlobalExceptionHandler.java` | `GlobalExceptionHandlerSecurityExceptionBugTest` (2) |
| 8 | Job de juros com taxa fixa, agendado 2x e reader singleton esgotável | `batch/config/BatchConfig.java` | `InterestRateCalculationBugTest` (2) |
| 9 | Transferência corrente→corrente usava validação de poupança e lançava exceção após salvar saldos | `service/CheckingAccountService.java` | `CheckingAccountTransferTransactionBugTest` (1) |

---

## 4. Correções de contrato descobertas ao ativar o H2

Ao reativar os testes de contexto, cinco defeitos reais de contrato apareceram:

| # | Bug | Fix |
|---|---|---|
| 10 | `@NotBlank` em campos `UUID` causava `UnexpectedTypeException` (500) em transferência | `DepositRequestDTO`: `@NotBlank` → `@NotNull` em `accountSends`/`accountReceive` |
| 11 | Rotas literais `/create` eram capturadas por `/{id}` e retornavam 403 | `SecurityConfiguration`: `ADMIN_GERENTE_CREATE_PATHS` avaliado antes dos padrões `/{id}` |
| 12 | Conta poupança inexistente retornava 400 em vez de 404 | `SavingsAccountService.getById`: `EntityNotFoundException` |
| 13 | Mapper não preenchia `isActive`, quebrando `INSERT` por `NULL not allowed` | `SavingsAccountMapper`: `@Mapping(target = "isActive", constant = "true")` |
| 14 | Transferência entre poupanças não era atômica e criava transação pelo validador de depósito | `SavingsAccountService.transferSavingsAccount`: `@Transactional` + `Transaction` explícita com `TRANSFERENCIA` |

> A correção **1** de build (MapStruct/Lombok) também pertence a este bloco de descobertas.

---

## 5. Novo ciclo — 8 bugs corrigidos

| # | Bug | Arquivo do fix | Teste(s) |
|---|---|---|---|
| 15 | `AlertAML(Transaction, String)` gravava o **id da transação** em `accountId` | `model/AlertAML.java` | `AlertAMLAccountIdBugTest` |
| 16 | `refreshToken` inválido retornava `null` → HTTP 200 com corpo vazio | `service/AuthenticationService.java` | `RefreshTokenInvalidTokenBugTest` |
| 17 | `.get()` sem `isPresent()` em validadores → `NoSuchElementException` (500) para conta inexistente | `utils/SavingsAccountValidate.java`, `utils/CheckingAccountValidate.java` | `MissingAccountOptionalBugTest` (4) |
| 18 | Depósito acima de R$ 10.000 era bloqueado, mas a mensagem dizia R$ 50.000 | `service/SavingsAccountService.java` | `SavingsDepositLimitMessageBugTest` |
| 19 | Saque usava `LocalDateTime.now()` (fuso do servidor) em vez de `America/Sao_Paulo` | `service/SavingsAccountService.java`, `service/CheckingAccountService.java` | `SavingsWithdrawalTimezoneBugTest` |
| 20 | `List.of("AGENCIA","ATM").contains(EnumOrigin)` nunca casava; todo depósito em dinheiro era rejeitado | `utils/DepositValidator.java` | `DepositValidatorOriginBugTest` (2) |
| 21 | Checagem de conta ativa invertida: ativa falhava, inativa concluía | `utils/TransactionValidate.java` | `TransactionValidateActiveAccountBugTest` (2) |
| 22 | Transferências validavam usuário usando **id da conta** em vez do **id do usuário** | `utils/SavingsAccountValidate.java`, `utils/CheckingAccountValidate.java` | `SavingsTransferActiveUserBugTest`, `CheckingTransferActiveUserBugTest` |

### Detalhes relevantes

- **AlertAML:** o alerta agora aponta para `accountReceive` (fallback `accountSends`) e não lança NPE quando a transação ainda não foi persistida.
- **Refresh token:** passa a lançar `BadCredentialsException` (subclasse de `AuthenticationException`), que o handler já mapeia para **401**.
- **Optional:** conta inexistente agora vira `EntityNotFoundException` → **404**, em vez de 500.
- **Origem do depósito:** o enum não possui `AGENCIA`/`ATM`; a validação passou a comparar corretamente com `DINHEIRO_FISICO`, a origem que chega nesse fluxo.
- **Dono da conta:** os validadores agora buscam a conta e usam `conta.getUser().getId()`, tanto na poupança quanto na conta corrente.

---

## 6. Candidato investigado e rejeitado

**“`Transaction` classifica alguns saques como `DEPOSITO`” (linhas 62–70 e 92–101).**

Investiguei todos os `new Transaction(...)` da aplicação:

- Os dois construtores que recebem `WithdrawRequestDTO` são usados **apenas** em fluxos de depósito:
  - `SavingsAccountService.deposit(...)`
  - `CheckingAccountService.deposit(...)`
  - detecção de fraude de depósito (`DepositValidatorService` e `SavingsAccountService`)
- Saques usam `WithdrawalRequestDTO`, cujo construtor define `EnumTransactionType.SAQUE`.

Conclusão: **não é bug**. O nome `WithdrawRequestDTO` é enganoso, mas o tipo gravado (`DEPOSITO`) está correto nos pontos de uso. Nenhuma alteração foi feita.

---

## 7. Evidência de execução

### Reprodutores do novo ciclo (vermelho → verde)

```bash
./mvnw -Dtest='AlertAMLAccountIdBugTest,RefreshTokenInvalidTokenBugTest,\
MissingAccountOptionalBugTest,SavingsDepositLimitMessageBugTest,\
SavingsWithdrawalTimezoneBugTest,DepositValidatorOriginBugTest,\
TransactionValidateActiveAccountBugTest,SavingsTransferActiveUserBugTest,\
CheckingTransferActiveUserBugTest' test
```

Antes dos fixes:

```
Tests run: 14, Failures: 13, Errors: 0, Skipped: 0
```

O 14º teste (`SavingsTransferActiveUserBugTest`) foi ajustado para simular o retorno real do JPA (`null` para usuário inexistente), ficando vermelho pelo motivo correto. Depois dos fixes:

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Suíte completa

```bash
./mvnw clean test
```

```
Tests run: 112, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Momento | Tests run | Falhas | Observação |
|---|---|---|---|
| Baseline inicial | 90 | 31 pré-existentes | Sem PostgreSQL em `localhost:5555` |
| Após fases 1–2 | 98 | 0 | Ainda dependia de infraestrutura |
| Com H2 + correções de contrato | 98 | 0 | Contexto completo verde |
| Após centralização inicial | 98 | 0 | 28 classes em `tests/` |
| Após novo ciclo | **112** | **0** | +14 reprodutores |
| Após reorganização por pacotes | **112** | **0** | Testes espelhando `src/main` |

---

## 8. Recomendações

1. Manter o profile `test` com H2 como padrão da suíte, e opcionalmente adicionar Testcontainers para testes de integração mais fiéis ao PostgreSQL.
2. Renomear `WithdrawRequestDTO` para algo como `AccountMovementRequestDTO`, já que ele é usado em depósitos e transferências.
3. Padronizar o uso de `America/Sao_Paulo` em **todas** as datas de domínio (idealmente via `Clock` injetável).
4. Revisar os demais pontos que ainda usam `LocalDateTime.now()` sem fuso (`AuditService`, `AlertAML`, `BankHoursService`).
5. Considerar substituir `userRepository.isActiveById` por consultas derivadas de `User`/`Account` com `Optional`, evitando confusão entre ids de conta e de usuário.
