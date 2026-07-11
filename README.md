# Email Service

Serviço de envio de e-mails com templates personalizáveis, processamento assíncrono via RabbitMQ e autenticação JWT. Construído com **Java 25** e **Spring Boot 4.1** seguindo princípios de Clean Architecture.

## Funcionalidades

- Cadastro e login de usuários com JWT
- CRUD de templates de e-mail com variáveis (`${nome}`) e pré-visualização ao vivo
- Envio assíncrono via fila (RabbitMQ) com retry automático (3 tentativas) e dead-letter queue
- Histórico de envios com status (`PENDING`, `RETRYING`, `SENT`, `FAILED`)
- Relatórios de envio por usuário e período: resumo com taxa de sucesso e evolução diária
- Interface web (HTML/CSS/JS separados, em `src/main/resources/static`)

## Arquitetura

```
com.emailservice
├── domain          # Entidades, repositórios e exceções (sem dependência de framework web)
│   ├── entity
│   ├── repository
│   └── exception
├── application     # Regras de negócio
│   ├── dto         # Records imutáveis
│   ├── port        # Interfaces (TokenService, TemplateRenderer, EmailQueueGateway)
│   └── usecase
├── infrastructure  # Implementações técnicas
│   ├── config      # Security, RabbitMQ, Web
│   ├── messaging   # Producer/Consumer AMQP
│   ├── security    # JWT, UserDetails adapter
│   └── service     # SMTP, renderização Thymeleaf
└── presentation    # Controllers REST + tratamento global de erros (RFC 9457)
```

**Fluxo de envio:** `POST /api/emails/send` → grava mensagem `PENDING` → publica na `email.queue` → consumer renderiza o template e envia via SMTP → status `SENT`. Em caso de falha, a mensagem vai para a fila de retry (TTL de 10s) e retorna à fila principal; após 3 tentativas, vai para a DLQ com status `FAILED`.

## Segurança

- Senhas com BCrypt; tokens JWT assinados (HS384) com expiração configurável
- Templates são validados antes da renderização: apenas expressões simples como
  `${variavel}` ou `${cliente.nome}` são permitidas, bloqueando server-side template
  injection (SSTI) via SpEL/OGNL
- Todos os recursos (templates e mensagens) são escopados ao usuário autenticado
- Front-end renderiza dados exclusivamente via `textContent` (sem `innerHTML`), prevenindo XSS
- Nenhuma credencial no código: SMTP e segredo JWT vêm de variáveis de ambiente

## Como rodar

### Pré-requisitos

- JDK 25
- Docker (para o RabbitMQ)

### Desenvolvimento

```bash
# Sobe apenas o RabbitMQ
docker compose up -d rabbitmq

# Configura as credenciais SMTP e um segredo JWT de desenvolvimento
# (ou use um application-dev.yml local com SPRING_PROFILES_ACTIVE=dev)
# PowerShell: $env:MAIL_USERNAME = "seu-email@gmail.com"
export MAIL_USERNAME=seu-email@gmail.com
export MAIL_PASSWORD=sua-senha-de-app
export JWT_SECRET=<base64 de pelo menos 48 bytes aleatórios>

./mvnw spring-boot:run
```

Acesse `http://localhost:8080` (a raiz redireciona para a interface web).

Para rodar sem RabbitMQ (somente API e templates, sem envio):

```bash
RABBITMQ_ENABLED=false ./mvnw spring-boot:run
```

### Docker Compose (aplicação completa)

Crie um arquivo `.env` na raiz (nunca commitá-lo):

```env
JWT_SECRET=<base64 de pelo menos 48 bytes aleatórios>
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-app
```

```bash
docker compose up --build
```

### Testes

```bash
./mvnw test
```

## Banco de dados (multi-driver)

A aplicação embarca os drivers JDBC de **H2, MySQL, PostgreSQL, SQL Server e
MariaDB**. O banco é escolhido apenas por variáveis de ambiente — sem recompilar.
O driver é detectado automaticamente a partir da `DB_URL`; o dialeto do Hibernate
também é resolvido automaticamente.

```env
# MySQL
DB_URL=jdbc:mysql://host:3306/emaildb

# PostgreSQL
DB_URL=jdbc:postgresql://host:5432/emaildb

# SQL Server
DB_URL=jdbc:sqlserver://host:1433;databaseName=emaildb;encrypt=true;trustServerCertificate=true

# MariaDB
DB_URL=jdbc:mariadb://host:3306/emaildb

DB_USERNAME=usuario
DB_PASSWORD=senha
```

Sem `DB_URL` definida, a aplicação usa H2 em arquivo (`./data/emaildb`).

## Variáveis de ambiente

| Variável           | Padrão              | Descrição                                                  |
|--------------------|---------------------|------------------------------------------------------------|
| `DB_URL`           | H2 em arquivo       | URL JDBC do banco (MySQL, PostgreSQL, SQL Server, MariaDB) |
| `DB_DRIVER`        | (auto pela URL)     | Força uma classe de driver JDBC específica                 |
| `DB_USERNAME`      | `sa`                | Usuário do banco                                           |
| `DB_PASSWORD`      | (vazio)             | Senha do banco                                             |
| `JPA_DDL_AUTO`     | `update`            | Estratégia de schema do Hibernate (`validate` em produção) |
| `JWT_SECRET`       | —                   | Segredo Base64 para assinar tokens (obrigatório)           |
| `JWT_EXPIRATION`   | `3600000`           | Validade do token em ms                                    |
| `MAIL_HOST`        | `smtp.gmail.com`    | Servidor SMTP                                              |
| `MAIL_PORT`        | `587`               | Porta SMTP (STARTTLS)                                      |
| `MAIL_USERNAME`    | —                   | Usuário SMTP (obrigatório)                                 |
| `MAIL_PASSWORD`    | —                   | Senha SMTP (obrigatório)                                   |
| `RABBITMQ_HOST`    | `localhost`         | Host do broker                                             |
| `RABBITMQ_ENABLED` | `true`              | Desativa a integração com o broker se `false`              |

## API

| Método | Rota                          | Descrição                                  |
|--------|-------------------------------|--------------------------------------------|
| POST   | `/api/auth/register`          | Cria usuário e retorna token               |
| POST   | `/api/auth/login`             | Autentica e retorna token                  |
| GET    | `/api/templates`              | Lista paginada de templates (`search`, `page`, `size`) |
| POST   | `/api/templates`              | Cria template                              |
| GET    | `/api/templates/{id}`         | Busca template por ID                      |
| GET    | `/api/templates/name/{name}`  | Busca template por nome                    |
| PUT    | `/api/templates/{id}`         | Atualiza template                          |
| DELETE | `/api/templates/{id}`         | Remove template                            |
| POST   | `/api/templates/preview`      | Renderiza HTML com variáveis de teste      |
| POST   | `/api/emails/send`            | Enfileira envio de e-mail (202 Accepted)   |
| GET    | `/api/emails`                 | Lista paginada de envios (`status`, `toEmail`, `page`, `size`) |
| GET    | `/api/emails/{id}`            | Consulta status de um envio                |
| GET    | `/api/reports/emails`         | Lista paginada dos envios do período, com usuário, template e datas |
| GET    | `/api/reports/emails/summary` | Resumo de envios do período (totais por status e taxa de sucesso) |
| GET    | `/api/reports/emails/daily`   | Envios agrupados por dia dentro do período |

### Anexos

`POST /api/emails/send` aceita o campo opcional `attachments`: uma lista de até 5 arquivos
(máximo de 10 MB no total), com o conteúdo codificado em Base64. Os anexos trafegam pela
fila junto com a mensagem e são adicionados ao e-mail no momento do envio SMTP.

```json
{
  "templateId": "0e2b7c31-8f4e-4d29-9a1f-53a2b8c90d11",
  "toEmail": "destinatario@example.com",
  "variables": { "nome": "Ana" },
  "attachments": [
    {
      "filename": "relatorio.pdf",
      "contentType": "application/pdf",
      "base64Content": "JVBERi0xLjQK..."
    }
  ]
}
```

### Relatórios

Os endpoints de relatório aceitam os parâmetros opcionais `startDate` e `endDate`
(formato ISO `yyyy-MM-dd`, filtrando pela data de criação do envio). Sem parâmetros, o
período padrão são os últimos 30 dias. Os dados são sempre escopados ao usuário autenticado.

`GET /api/reports/emails` retorna a lista detalhada e paginada, ordenada do envio mais
recente para o mais antigo. Parâmetros adicionais: `status` (PENDING, RETRYING, SENT,
FAILED), `page` (padrão 0) e `size` (padrão 20, máximo 100).

```
GET /api/reports/emails?startDate=2026-07-01&endDate=2026-07-08&status=SENT&page=0&size=20
```

```json
{
  "content": [
    {
      "id": "c87eb389-1fa8-4996-9b06-9c16b3a2aa4c",
      "username": "lucas",
      "userEmail": "lucas@example.com",
      "templateId": "0e2b7c31-8f4e-4d29-9a1f-53a2b8c90d11",
      "templateName": "boas-vindas",
      "toEmail": "destinatario@example.com",
      "subject": "Bem-vindo!",
      "status": "SENT",
      "retryCount": 0,
      "sentAt": "2026-07-08T22:43:05",
      "createdAt": "2026-07-08T22:42:55"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

```
GET /api/reports/emails/summary?startDate=2026-07-01&endDate=2026-07-08
```

```json
{
  "startDate": "2026-07-01",
  "endDate": "2026-07-08",
  "total": 6,
  "sent": 3,
  "failed": 1,
  "pending": 1,
  "retrying": 1,
  "successRate": 50.0
}
```

`GET /api/reports/emails/daily` retorna uma lista com a mesma contagem por status para
cada dia que teve envios, em ordem cronológica.

Erros seguem o formato *problem detail* (RFC 9457). Uma coleção Postman está disponível em `postman/`.
