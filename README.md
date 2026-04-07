# Task Management System

Sistema de gerenciamento de tarefas e boards construído com **Java 21**, **Gradle**, **Flyway** e **Hibernate JPA**.

## 🚀 Funcionalidades

- **Gerenciamento de Tarefas**: Criar, atualizar, completar e reabrir tarefas
- **Prioridades**: LOW (Baixa), MEDIUM (Média), HIGH (Alta), URGENT (Urgente)
- **Sistema de Boards**: Boards com colunas e cards estilo Kanban
- **Movimentação de Cards**: Fluxo sequencial entre colunas sem pular etapas
- **Bloqueio/Desbloqueio**: Cards podem ser bloqueados com justificativa
- **Relatórios**: Tempo de conclusão e histórico de bloqueios
- **Migrações de Banco de Dados**: Flyway para versionamento do schema
- **Logging**: SLF4J/Logback com logs em console e arquivo
- **Testes**: Testes unitários e de integração com JUnit 5

## 📋 Requisitos

- Java 21+
- MySQL 8.0+
- Gradle 9.2.0+

## 🛠️ Configuração

### 1. Configurar Banco de Dados

Edite o arquivo `app/src/main/resources/database.properties` com suas credenciais do MySQL:

```properties
db.url=jdbc:mysql://localhost:3306/task_management
db.username=seu_usuario
db.password=sua_senha
```

> **⚠️ Importante**: O arquivo `database.properties` está no `.gitignore` para evitar commit de credenciais.

### 2. Criar o Banco de Dados

```sql
CREATE DATABASE task_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 🏃 Executando a Aplicação

### Executar migrações do banco de dados:

```bash
./gradlew run --args="migrate"
```

### Executar a aplicação principal:

```bash
./gradlew run
```

## 🧪 Testes

### Executar todos os testes:

```bash
./gradlew test
```

### Executar build completo:

```bash
./gradlew build
```

## 📁 Estrutura do Projeto

```
task-management/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/task/management/
│   │   │   │   ├── App.java                     # Ponto de entrada
│   │   │   │   ├── domain/                      # Entidades JPA
│   │   │   │   │   ├── Board.java
│   │   │   │   │   ├── BoardColumn.java
│   │   │   │   │   ├── Card.java
│   │   │   │   │   ├── Blockade.java
│   │   │   │   │   ├── ColumnHistory.java
│   │   │   │   │   ├── ColumnType.java
│   │   │   │   │   ├── CardStatus.java
│   │   │   │   │   └── Task.java
│   │   │   │   ├── service/                     # Regras de negócio
│   │   │   │   │   ├── BoardService.java
│   │   │   │   │   ├── CardService.java
│   │   │   │   │   └── ReportService.java
│   │   │   │   ├── infrastructure/              # Persistência e JPA
│   │   │   │   │   ├── jpa/JpaUtil.java
│   │   │   │   │   └── repository/
│   │   │   │   ├── migrations/                  # Migrações Flyway
│   │   │   │   │   ├── MigrationExecutor.java
│   │   │   │   │   ├── MigrationRunnerFactory.java
│   │   │   │   │   └── MigrationService.java
│   │   │   │   └── ui/                          # Interface CLI
│   │   │   │       └── BoardMenu.java
│   │   │   └── resources/
│   │   │       ├── META-INF/persistence.xml     # Configuração JPA
│   │   │       ├── db/migration/                # Scripts de migração
│   │   │       │   ├── V1__create_tasks_table.sql
│   │   │       │   └── V2__create_board_tables.sql
│   │   │       ├── database.properties          # Credenciais (não versionado)
│   │   │       └── logback.xml                  # Configuração de logging
│   │   └── test/
│   │       └── java/task/management/
│   │           ├── AppTest.java
│   │           ├── domain/TaskTest.java
│   │           └── migrations/MigrationIntegrationTest.java
│   └── build.gradle.kts
├── docs/
├── logs/
└── README.md
```

## 🗄️ Banco de Dados

### Migrações

O projeto usa Flyway para gerenciar versões do banco de dados. As migrações estão em `app/src/main/resources/db/migration/`:

#### V1 - Tabela de Tarefas
Cria a tabela `tasks` com: `id`, `title`, `description`, `completed`, `priority`, `created_at`, `updated_at`.

#### V2 - Tabelas do Board
Cria a estrutura completa de boards:
- **boards**: Tabela principal de boards
- **columns**: Colunas do board (INITIAL, PENDING, FINAL, CANCELLED)
- **cards**: Cards associados às colunas
- **blockades**: Registros de bloqueio/desbloqueio
- **column_history**: Histórico de movimentação entre colunas

## 📝 Logging

O projeto usa SLF4J com Logback. Configuração em `logback.xml`:

| Destino | Configuração |
|---------|-------------|
| Console | Logs em tempo real |
| Arquivo | `logs/task-management.log` com rotação diária |

### Níveis de Log

| Pacote | Nível |
|--------|-------|
| `task.management` | INFO |
| `org.flywaydb` | INFO |
| `org.hibernate` | WARN |

## 🔐 Segurança

- ✅ Credenciais de banco fora do version control
- ✅ Arquivo `database.properties` ignorado pelo Git
- ✅ Configurações centralizadas em arquivo properties

## 🏗️ Arquitetura

### Camadas

```
UI (CLI) → Service → Repository → JPA/Hibernate → MySQL
```

### Padrões Utilizados

- **Repository**: Encapsulamento de acesso a dados
- **Service**: Encapsulamento de regras de negócio
- **Factory**: Criação de serviços com configuração centralizada
- **Entity**: Entidades JPA com validações e comportamento

### Fluxo de Migrações

```
MigrationExecutor → MigrationRunnerFactory → MigrationService → Flyway
```

As configurações são carregadas exclusivamente do arquivo `database.properties`.

## 🧩 Dependências

### Produção

| Dependência | Versão | Finalidade |
|-------------|--------|------------|
| SLF4J API | 2.0.12 | Logging API |
| Logback | 1.5.3 | Implementação de logging |
| MySQL Connector | 8.0.33 | Driver JDBC |
| Flyway Core | 12.3.0 | Migrações de banco de dados |
| Flyway MySQL | 12.3.0 | Suporte MySQL para Flyway |
| Jakarta Persistence API | 3.1.0 | JPA API |
| Hibernate Core | 6.4.4.Final | Implementação JPA |

### Teste

| Dependência | Versão | Finalidade |
|-------------|--------|------------|
| JUnit Jupiter | - | Framework de testes |
| H2 Database | 2.2.224 | Banco em memória para testes |

## 📊 Cobertura de Testes

| Teste | Descrição |
|-------|-----------|
| `TaskTest` | 14 testes cobrindo todas as operações da entidade Task |
| `AppTest` | 2 testes para validação da aplicação principal |
| `MigrationIntegrationTest` | 4 testes para validar migrações e persistência JPA |

## 📖 Documentação Adicional

- [Documentação de Migrations](docs/annotation/migrations.md)
- [Documentação de Flyway](docs/annotation/flyway.md)
- [Recomendações](docs/annotation/recomendacoes.md)
- [Implementação do Board](docs/annotation/board.md)

## 👥 Contribuindo

1. Configure suas credenciais de banco de dados no `database.properties`
2. Execute as migrações: `./gradlew run --args="migrate"`
3. Execute os testes: `./gradlew test`
4. Execute a aplicação: `./gradlew run`

## 👤 Autor

**Allan Giaretta**
