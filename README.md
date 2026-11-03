# 🏦 Sistema Bancário Simplificado

![Versão](https://img.shields.io/badge/vers%C3%A3o-1.4.0-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-required-blue)

API REST de um sistema bancário simplificado, com contas poupança e corrente, transferências, agendamento de transferências, controle de depósitos suspeitos, alertas AML publicados no Kafka e juros em lote.

> ⚠️ **Atenção:** esta versão tem problemas de segurança e de funcionamento. Leia a seção [Problemas conhecidos](#-problemas-conhecidos) antes de usar ou implantar o projeto.

---

## 📋 Índice

- [Funcionalidades](#-funcionalidades)
- [Autenticação e perfis](#-autenticação-e-perfis)
- [Regras de negócio](#-regras-de-negócio)
- [Tecnologias](#-tecnologias)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração](#-configuração)
- [Instalação e execução](#-instalação-e-execução)
- [Enums](#-enums)
- [Endpoints](#-endpoints)
- [Campos dos DTOs](#-campos-dos-dtos)
- [Exemplos de requisição](#-exemplos-de-requisição)
- [Erros](#-erros)
- [Problemas conhecidos](#-problemas-conhecidos)
- [Testes](#-testes)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [Changelog](#-changelog)
- [Contribuindo](#-contribuindo)
- [Autor](#-autor)

---

## ✨ Funcionalidades

- **Usuários:** cadastro com username e e-mail únicos, senha criptografada com BCrypt, atualização, troca de papel e ativação ou desativação.
- **Contas:** criação de contas poupança (`SavingsAccount`) e corrente (`CheckingAccount`) vinculadas a um usuário, com saldo inicial mínimo de R$ 100 e CPF validado. Também é possível ativar e desativar contas.
- **Depósitos e saques:** existem endpoints, mas a maioria tem problemas. Veja [Regras de negócio](#-regras-de-negócio) e [Problemas conhecidos](#-problemas-conhecidos).
- **Transferências:** endpoint `POST /transfer`, que move saldo entre contas poupança e corrente com lock por conta.
- **Agendamentos:** transferências futuras entre contas poupança, executadas por um job a cada 5 minutos.
- **Transações:** consulta por ID e marcação como concluída ou com falha.
- **Alertas AML:** registrados no banco e, em alguns casos, publicados no tópico Kafka `alertas-aml`.
- **Juros em lote:** job Spring Batch que aplica juros às contas poupança todo dia às 23h59.
- **Autenticação JWT:** access token e refresh token. O login está quebrado (ver problemas).

## 👥 Autenticação e perfis

O fluxo esperado é:

1. Fazer login em `POST /authentication/signin` e receber `token` e `refreshToken`.
2. Enviar o token em todas as rotas protegidas:

   ```http
   Authorization: Bearer <token>
   ```

3. Quando o access token expirar, usar `POST /authentication/refresh`. A resposta traz um novo access token e devolve o mesmo refresh token.

| Token | Validade |
|-------|----------|
| Access token | 24 minutos |
| Refresh token | 7 dias |

> ⚠️ **O login não funciona nesta versão.** Veja o primeiro item em [Problemas conhecidos](#-problemas-conhecidos).

### Perfis

Os perfis são `ADMIN`, `GERENTE` e `CLIENTE`. Na prática, porém, a configuração de segurança faz com que a maior parte das rotas fique acessível apenas a um perfil. Veja a tabela de [Endpoints](#-endpoints), que mostra as permissões efetivas.

## 📏 Regras de negócio

### Depósitos em poupança (`PUT /savings-accounts/{id}/deposit`)

| Situação | Comportamento |
|----------|---------------|
| Valor acima de R$ 50.000 | Gera alerta AML e responde `400` com a mensagem de limite. |
| Valor acima de R$ 10.000 | Grava a transação como `PENDENTE` e gera alerta, mas responde `400`. A mensagem cita o limite de R$ 50.000. |
| Três depósitos de mesmo valor em menos de 5 minutos | Grava a transação como `PENDENTE` e gera alerta. A requisição responde com sucesso, mas o saldo **não** é creditado. |
| Origem `TED` | Aceita em qualquer horário. |
| Origem `DOC` | Aceita somente até R$ 4.999,99 e antes das 17h. |
| Origem `DINHEIRO_FISICO` | Sempre recusada. Veja [Problemas conhecidos](#-problemas-conhecidos). |
| Origem `SAQUE` | Recusada em depósito. |

### Depósitos em corrente (`PUT /checking-accounts/{id}/deposit`)

Não funciona nesta versão. A validação está invertida e todo depósito falha. Veja [Problemas conhecidos](#-problemas-conhecidos).

### Saques em poupança (`PUT /savings-accounts/{id}/withdrawal`)

| Regra | Comportamento |
|-------|---------------|
| Valor positivo | Obrigatório. |
| Saldo | O saque não pode ser maior que o saldo. |
| Limite em 24 horas | A soma dos saques das últimas 24 horas não pode passar de R$ 2.000. |
| Valor acima de R$ 20.000 | Grava a transação como `PENDENTE`, mas **o saldo é debitado mesmo assim**. |

### Saques em corrente

Não funcionam nesta versão, pelo mesmo motivo dos depósitos.

### Transferências (`POST /transfer`)

- Move saldo entre contas poupança e corrente, usando `fromType` e `toType` (`SAVING` ou `CHECKING`).
- Exige saldo suficiente na conta de origem.
- Não verifica se o usuário autenticado é dono da conta de origem.
- Não valida se o valor é positivo. Veja [Problemas conhecidos](#-problemas-conhecidos).

### Transferências entre poupanças (`PUT /savings-accounts/transfer/savingsAccount`)

Movem o saldo, mas o registro da transação sai com status `FALHOU` quando a conta de destino está ativa, e com tipo `DEPOSITO`. Veja [Problemas conhecidos](#-problemas-conhecidos).

### Transferências agendadas

| Regra | Comportamento |
|-------|---------------|
| Entre quais contas | Somente entre contas poupança. |
| Data | Precisa ser futura (`@Future`). |
| Execução | Um job roda a cada 5 minutos e executa as transferências com status `PENDENTE` cuja data já passou. |
| Saldo insuficiente | Marca como `FALHOU` e incrementa o contador de tentativas. |
| Reprocessamento | Permitido apenas para status `FALHOU`. Atualiza a data para o momento atual. |
| Cancelamento | Não é permitido para transferências `CONCLUIDA`. |

| Status | Significado |
|--------|-------------|
| `PENDENTE` | Aguardando a data agendada. |
| `CONCLUIDA` | Executada com sucesso. |
| `FALHOU` | Não executada. Pode ser reprocessada. |
| `CANCELADA` | Cancelada antes da execução. |

### Juros

Todo dia às 23h59, o job aplica juros sobre o saldo de **todas** as contas poupança. A taxa de 0,5% ao mês é dividida por 30 para o cálculo diário. Veja [Problemas conhecidos](#-problemas-conhecidos) sobre a taxa definida no modelo.

## 🛠 Tecnologias

- Java 17
- Spring Boot 3.4.3: Web, Data JPA, Security, Validation, Batch, Scheduling
- Spring Kafka
- PostgreSQL
- JWT (jjwt 0.11.5)
- Lombok e MapStruct
- JUnit 5, Mockito e Spring Security Test

## ✅ Pré-requisitos

- JDK 17 ou superior
- Maven 3.8+ (ou o wrapper `./mvnw`)
- PostgreSQL
- Kafka (para os alertas AML)
- Docker e Docker Compose (opcional, para subir o Kafka)

## ⚙️ Configuração

Edite `src/main/resources/application.yml` ou, de preferência, use variáveis de ambiente:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5555/banco
    username: username
    password: password
  kafka:
    bootstrap-servers: localhost:9092

jwt:
  secret: "troque-por-um-segredo-forte"
```

> ⚠️ Não versione o segredo JWT nem senhas reais. O valor do exemplo no repositório é `password`.

### Usuário administrador padrão

Ao iniciar, a aplicação cria automaticamente um usuário `admin` com senha `admin`, caso não exista nenhum usuário com papel `ADMIN`. **Troque essa senha antes de qualquer uso real.**

### Banco de dados

Crie o banco e execute os scripts de `src/main/resources/tables/` nesta ordem:

1. `V1__create_SavingsAccount.sql`
2. `V2__create_CheckingAccount.sql`
3. `V3__create_users.sql`
4. `V4__create_transaction.sql`
5. `V5__create_AlertAML.sql`
6. `V6__create__scheduled_transfer.sql`
7. `V7__create_batch_tables.sql`

O projeto não usa Flyway, então os scripts são executados manualmente.

## 🚀 Instalação e execução

1. Clone o repositório e acesse a versão 1.4.0:

   ```bash
   git clone https://github.com/Kiraruto/Sistema-Bancario.git
   cd Sistema-Bancario
   git checkout 1.4.0
   ```

2. (Opcional) Suba o Kafka. O `docker-compose.yml` fica em `src/main/resources/`:

   ```bash
   docker network create bank-network
   docker compose -f src/main/resources/docker-compose.yml up -d
   ```

3. Compile e execute:

   ```bash
   ./mvnw clean package
   ./mvnw spring-boot:run
   ```

   Ou execute o JAR gerado:

   ```bash
   java -jar target/Sistema-bancario-0.0.1-SNAPSHOT.jar
   ```

A API sobe na porta `8080`, porque o projeto não define outra.

## 🧾 Enums

| Enum | Valores |
|------|---------|
| `EnumUserRole` | `ADMIN`, `GERENTE`, `CLIENTE` |
| `EnumMaritalStatus` | `SOLTEIRO`, `CASADO`, `SEPARADO`, `DIVORCIADO`, `VIUVO` |
| `EnumTypeDocument` | `RG`, `CNH` |
| `EnumOrigin` | `TED`, `DOC`, `DINHEIRO_FISICO`, `SAQUE` |
| `EnumTransactionType` | `TRANSFERENCIA`, `DEPOSITO`, `SAQUE` |
| `EnumStatus` | `PENDENTE`, `CONCLUIDA`, `FALHOU`, `CANCELADA` |
| `AccountType` | `CHECKING`, `SAVING` |
| `EnumStatusAlert` | `NOVO`, `EM_ANALISE`, `FECHADO_FALSO_POSITIVO`, `FECHADO_REPORTADO` |

## 📡 Endpoints

A tabela mostra as **permissões efetivas**. Elas diferem do que as listas de `SecurityConfiguration` sugerem, porque o Spring aplica a primeira regra que bate com a rota. Veja [Problemas conhecidos](#-problemas-conhecidos), item 3.

### Autenticação

| Método | Rota | Acesso efetivo | Corpo |
|--------|------|----------------|-------|
| POST | `/authentication/signin` | ⚠️ Ninguém (retorna `401`) | `SigninRequest` |
| POST | `/authentication/refresh` | ⚠️ Ninguém (retorna `401`) | `RefreshTokenRequest` |

### Usuários (`/users`)

| Método | Rota | Acesso efetivo | Corpo |
|--------|------|----------------|-------|
| GET | `/users` | ADMIN | |
| GET | `/users/{id}` | ADMIN | |
| POST | `/users` | ADMIN | `UserDTO` |
| PUT | `/users/{id}/update` | ADMIN | `UserDTO` |
| PUT | `/users/{id}/admin` | ADMIN | |
| PUT | `/users/{id}/gerente` | ADMIN | |
| PUT | `/users/{id}/cliente` | ADMIN | |
| PUT | `/users/{id}/disableUser` | ADMIN | |
| PUT | `/users/{id}/activateUser` | ADMIN | |

### Poupança (`/savings-accounts`)

| Método | Rota | Acesso efetivo | Corpo |
|--------|------|----------------|-------|
| POST | `/savings-accounts/create` | GERENTE | `SavingsAccountDTO` |
| GET | `/savings-accounts` | GERENTE | |
| GET | `/savings-accounts/{id}` | GERENTE | |
| GET | `/savings-accounts/{id}/balance` | GERENTE | |
| GET | `/savings-accounts/{id}/transactions` | GERENTE | |
| PUT | `/savings-accounts/{id}/deposit` | GERENTE | `WithdrawRequestDTO` |
| PUT | `/savings-accounts/{id}/withdrawal` | GERENTE | `WithdrawalRequestDTO` |
| PUT | `/savings-accounts/transfer/savingsAccount` | GERENTE | `DepositRequestDTO` |
| PUT | `/savings-accounts/{id}/disableSavingsAccount` | GERENTE | |
| PUT | `/savings-accounts/{id}/activateSavingsAccount` | GERENTE | |

### Corrente (`/checking-accounts`)

| Método | Rota | Acesso efetivo | Corpo |
|--------|------|----------------|-------|
| POST | `/checking-accounts/create` | GERENTE | `CheckingAccountDTO` |
| GET | `/checking-accounts` | GERENTE | |
| GET | `/checking-accounts/{id}` | GERENTE | |
| GET | `/checking-accounts/{id}/balance` | GERENTE | |
| GET | `/checking-accounts/{id}/transactions` | GERENTE | |
| PUT | `/checking-accounts/{id}/deposit` | GERENTE ⚠️ falha sempre | `WithdrawRequestDTO` |
| PUT | `/checking-accounts/{id}/withdrawal` | GERENTE ⚠️ falha sempre | `WithdrawalRequestDTO` |
| PUT | `/checking-accounts/transfer/checkingAccount` | GERENTE ⚠️ falha sempre | `DepositRequestDTO` |
| PUT | `/checking-accounts/{id}/disableCheckingAccount` | GERENTE | |
| PUT | `/checking-accounts/{id}/activateCheckingAccount` | GERENTE | |

### Transferências (`/transfer`)

| Método | Rota | Acesso efetivo | Corpo |
|--------|------|----------------|-------|
| POST | `/transfer` | GERENTE, CLIENTE | `TransferRequestDTO` |

### Agendamentos (`/schedule`)

| Método | Rota | Acesso efetivo | Corpo |
|--------|------|----------------|-------|
| POST | `/schedule` | GERENTE | `ScheduledTransferDTO` |
| GET | `/schedule` | GERENTE | |
| GET | `/schedule/{id}` | GERENTE | |
| GET | `/schedule/user/{userId}` | GERENTE | |
| GET | `/schedule/{id}/pending` | GERENTE | ID do **usuário** |
| GET | `/schedule/{id}/status` | GERENTE | |
| GET | `/schedule/failed` | GERENTE | |
| PUT | `/schedule/{id}` | GERENTE | `ScheduledTransferDTO` |
| PUT | `/schedule/{id}/cancel` | GERENTE | |
| PUT | `/schedule/{id}/retry/` | GERENTE | |
| DELETE | `/schedule/{id}` | GERENTE | |

### Transações (`/transaction`)

| Método | Rota | Acesso efetivo |
|--------|------|----------------|
| GET | `/transaction/{id}` | ADMIN |
| PUT | `/transaction/full/{id}` | ADMIN |
| PUT | `/transaction/failure/{id}` | ADMIN |

## 🗂 Campos dos DTOs

| DTO | Campos | Observações |
|-----|--------|-------------|
| `UserDTO` | `username`, `firstName`, `lastName`, `password`, `email` | Todos obrigatórios. `email` precisa ser válido. |
| `SavingsAccountDTO` | `fullName`, `dateBirth`, `address`, `maritalStatus`, `phoneNumber`, `email`, `cpf`, `rgOrCnh`, `documentNumber`, `balance`, `userId` | `dateBirth` não pode ser futura. `phoneNumber` tem até 11 caracteres. `cpf` precisa ser válido. `documentNumber` tem até 20 caracteres. `balance` mínimo: R$ 100. |
| `CheckingAccountDTO` | Os mesmos campos de `SavingsAccountDTO`, mais `hasOverdraft` e `requiresIncomeProof` | `documentNumber` tem até 11 caracteres. Os demais limites são iguais. |
| `WithdrawRequestDTO` | `idAccount`, `amount`, `enumOrigin` | Usado em depósitos. `amount` precisa ser positivo, validado no serviço. |
| `WithdrawalRequestDTO` | `idAccount`, `amount`, `enumOrigin` | Usado em saques. |
| `DepositRequestDTO` | `fullName`, `email`, `cpf`, `balance`, `origin`, `accountSends`, `accountReceive`, `description`, `userId` | Usado nas transferências entre contas. O valor vai no campo `balance`. |
| `TransferRequestDTO` | `fromId`, `fromType`, `toId`, `toType`, `amount` | Sem validação de anotação. |
| `ScheduledTransferDTO` | `accountSends`, `accountReceive`, `amount`, `scheduledDate`, `userId` | `amount` precisa ser positivo. `scheduledDate` precisa ser futura. |
| `SigninRequest` | `username`, `password` | |
| `RefreshTokenRequest` | `token` | Recebe o refresh token. |

## 💡 Exemplos de requisição

### Cadastrar um usuário

```http
POST /users
Content-Type: application/json

{
  "username": "joao",
  "firstName": "João",
  "lastName": "Silva",
  "password": "senha123",
  "email": "joao@exemplo.com"
}
```

### Criar conta poupança

```http
POST /savings-accounts/create
Content-Type: application/json

{
  "fullName": "João Silva",
  "dateBirth": "1990-05-15",
  "address": "Rua das Flores, 100",
  "maritalStatus": "SOLTEIRO",
  "phoneNumber": "81999999999",
  "email": "joao@exemplo.com",
  "cpf": "12345678909",
  "rgOrCnh": "RG",
  "documentNumber": "1234567",
  "balance": 1000.00,
  "userId": "7f3c1d2e-0000-0000-0000-000000000000"
}
```

### Depósito em poupança

```http
PUT /savings-accounts/{id}/deposit
Content-Type: application/json

{
  "idAccount": "7f3c1d2e-0000-0000-0000-000000000000",
  "amount": 500.00,
  "enumOrigin": "TED"
}
```

### Saque em poupança

```http
PUT /savings-accounts/{id}/withdrawal
Content-Type: application/json

{
  "idAccount": "7f3c1d2e-0000-0000-0000-000000000000",
  "amount": 200.00,
  "enumOrigin": "SAQUE"
}
```

### Transferência entre contas

```http
POST /transfer
Content-Type: application/json

{
  "fromId": "7f3c1d2e-0000-0000-0000-000000000000",
  "fromType": "SAVING",
  "toId": "a1b2c3d4-0000-0000-0000-000000000000",
  "toType": "CHECKING",
  "amount": 150.00
}
```

### Agendar transferência

```http
POST /schedule
Content-Type: application/json

{
  "accountSends": "7f3c1d2e-0000-0000-0000-000000000000",
  "accountReceive": "b2c3d4e5-0000-0000-0000-000000000000",
  "amount": 300.00,
  "scheduledDate": "2026-12-01T10:00:00",
  "userId": "7f3c1d2e-0000-0000-0000-000000000000"
}
```

## ⚠️ Erros

As respostas de erro usam o formato `ErroResponse` (`status`, `message` e, para validação, `erros`).

| Status | Quando acontece |
|--------|-----------------|
| `400` | Regras de negócio (a maioria das exceções, inclusive "conta não encontrada") e corpo mal formado. |
| `401` | Sem token ou credenciais inválidas. O corpo é `{"error": "Não autorizado..."}`. Token expirado tem outro formato de corpo. |
| `403` | Acesso negado pela autorização. |
| `404` | Rota não encontrada e algumas exceções de "não encontrado" (como transações pendentes). |
| `405` | Método HTTP não suportado pela rota. |
| `409` | Duplicidade, como usuário ou e-mail já cadastrado. |
| `422` | Falha de validação do corpo (`@Valid`), com a lista de campos em `erros`. |
| `500` | Erro não tratado. Também retornado por `IllegalStateException`, usada na validação de depósitos acima de R$ 10.000 em conta corrente. |

## 🐞 Problemas conhecidos

Os itens abaixo foram confirmados lendo o código. Ainda não foram testados em execução.

1. **Login quebrado.** A rota pública de autenticação é `/auth/**`, mas o controller usa `/authentication`. Assim, `POST /authentication/signin` e `/refresh` não são públicos e retornam `401`. Não é possível obter token sem um já existir.
2. **Usuário padrão com senha conhecida.** A aplicação cria `admin` com senha `admin` na primeira execução, se não houver um `ADMIN`.
3. **Permissões efetivas.** O Spring aplica a primeira regra que bate com a rota. Como `ADMIN_PATHS` vem antes das outras listas, `/users/**` e `/transaction/**` ficam só com ADMIN. `GERENTE_PATHS` vem antes das regras de `CLIENTE`, então `/savings-accounts/**`, `/checking-accounts/**` e `/schedule/**` ficam só com GERENTE. Consequências:
   - O perfil `CLIENTE` não acessa nenhuma conta, saldo ou extrato. Só acessa `/transfer`.
   - O perfil `ADMIN` não acessa contas nem agendamentos.
   - `GERENTE` não consegue desativar ou ativar usuários.
4. **`/transfer` sem verificação de dono.** Qualquer usuário com perfil `GERENTE` ou `CLIENTE` pode movimentar saldo de qualquer conta, informando o `fromId`. Além disso, o valor não é validado como positivo, então um `amount` negativo aumentaria o saldo da conta de origem.
5. **Contas correntes quebradas.** A validação de `CheckingAccountValidate` está invertida: `validateCheckingAccountWithdraw` e `validateCheckingAccountWithdrawal` lançam erro quando a conta **existe**. Por isso depósitos e saques em corrente sempre falham. `validateDepositChecking` rejeita todo valor positivo, então `/checking-accounts/transfer/checkingAccount` também falha sempre.
6. **Depósitos em poupança com origem limitada.** `DINHEIRO_FISICO` é comparado com uma lista de strings (`"AGENCIA"`, `"ATM"`), e a comparação nunca é verdadeira. Por isso depósitos em dinheiro sempre falham. `SAQUE` não é aceito em depósito.
7. **Depósito suspeito retorna sucesso sem creditar.** No caso de três depósitos iguais em menos de 5 minutos, a poupança grava a transação como `PENDENTE`, gera o alerta e encerra o método sem alterar o saldo, respondendo sem erro.
8. **Depósito acima de R$ 10.000 em poupança cria registro e responde erro.** A transação fica `PENDENTE`, mas a resposta é `400`, e a mensagem cita R$ 50.000.
9. **Saque acima de R$ 20.000 é debitado mesmo `PENDENTE`.** O status é gravado, mas o saldo é alterado de qualquer forma.
10. **Horário de TED não aplicado.** `BankHoursService.isTedTimeValid` calcula se é dia útil, mas o resultado é ignorado em `DepositValidator.validateTed()`.
11. **Transferências entre poupanças com registro incorreto.** A condição que define `FALHOU` está invertida: quando a conta de destino está **ativa**, a transação é gravada como `FALHOU`, embora o dinheiro seja movido. O tipo gravado é `DEPOSITO`. Como o registro é criado depois de alterar os saldos, uma falha na validação de origem pode deixar o saldo alterado sem registro. Não confirmei se o método é transacional.
12. **Validação de UUID incorreta.** `DepositRequestDTO` usa `@NotBlank` em campos `UUID`. Essa anotação só se aplica a `String`, então a validação pode gerar erro interno.
13. **Validação de usuário usando ID de conta.** `SavingsAccountValidate.validateDepositSavings` verifica o usuário com `accountSends` e `accountReceive`, que são IDs de conta, não de usuário.
14. **`FraudDetectionService` não é usado.** Nenhum código o chama. A detecção de fraude efetiva está em `SavingsAccountService.deposit` e em `DepositValidatorService`.
15. **Taxa de juros inconsistente.** O modelo `SavingsAccount` define `interestRate` como 0,6743%, mas o job de juros usa 0,5% ao mês, e o campo do modelo não é usado no cálculo.
16. **Swagger não configurado.** `SecurityConfiguration` libera `/swagger-ui/**` e `/v3/api-docs/**`, mas o `pom.xml` não tem `springdoc`.
17. **`/schedule/{id}/pending` recebe ID de usuário,** não de agendamento.
18. **Migrations manuais.** Os scripts não são executados automaticamente.
19. **Valores de exemplo.** O segredo JWT e as credenciais do banco no `application.yml` são de exemplo. O usuário `admin`/`admin` é criado automaticamente.

## 🧪 Testes

```bash
./mvnw test
```

O projeto tem 86 métodos `@Test` em 10 classes, cobrindo serviços (contas, transações, usuários e auditoria), controllers (usuários, poupança e transações), o job de juros e o contexto da aplicação. Os testes não foram executados nesta revisão.

## 📁 Estrutura do projeto

```text
src/main/java/com/github/kiraruto/sistemaBancario/
├── Application.java
├── batch/config/        # Job de juros (Spring Batch)
├── config/              # Segurança, JWT, agendamento e filtro JWT
├── controller/          # Endpoints REST
├── dto/                 # Entrada e saída da API
├── exceptions/          # Exceções de negócio
├── global/exception/    # Tratamento global de erros
├── mapper/              # MapStruct
├── model/               # Entidades e enums
├── repository/          # Spring Data JPA
├── service/             # Regras de negócio
└── utils/               # Validadores
```

## 📜 Changelog

### 1.4.0
- [Descreva as mudanças desta versão.]

## 🤝 Contribuindo

1. Faça um fork do projeto.
2. Crie uma branch (`git checkout -b feature/nova-feature`).
3. Faça commit das alterações (`git commit -m 'Adiciona nova feature'`).
4. Envie a branch (`git push origin feature/nova-feature`).
5. Abra um Pull Request.

## 👤 Autor

**Kiraruto**
- GitHub: [@Kiraruto](https://github.com/Kiraruto)
