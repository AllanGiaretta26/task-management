# Task Management System

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Gradle](https://img.shields.io/badge/Gradle-9.2.0-02303A?logo=gradle)
![Hibernate](https://img.shields.io/badge/Hibernate-6.4.4-59666C?logo=hibernate)
![Flyway](https://img.shields.io/badge/Flyway-12.3.0-CC0200?logo=flyway)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)
![License](https://img.shields.io/badge/licença-MIT-blue)

> Sistema de gerenciamento de tarefas e boards no estilo Kanban, com interface via terminal, controle de versão do banco de dados e rastreamento completo de histórico.

---

## 📋 Descrição

O **Task Management System** é uma aplicação CLI desenvolvida em Java que combina um gerenciador de tarefas tradicional com um sistema de boards no estilo Kanban. Ele permite criar boards com colunas personalizadas, mover cards entre etapas de forma sequencial, bloquear cards com justificativa e gerar relatórios sobre o fluxo de trabalho.

O projeto foi criado para demonstrar boas práticas de arquitetura em camadas, uso de ORM com Hibernate JPA, migrações de banco de dados versionadas com Flyway e testes automatizados com JUnit 5.

---

## 🚦 Status do Projeto

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

O projeto está em desenvolvimento ativo. As funcionalidades principais estão implementadas e testadas.

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Finalidade |
|---|---|---|
| ![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white) | 21 | Linguagem principal |
| ![Gradle](https://img.shields.io/badge/Gradle-9.2.0-02303A?logo=gradle) | 9.2.0 | Build e gerenciamento de dependências |
| ![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white) | 8.0+ | Banco de dados relacional |
| ![Hibernate](https://img.shields.io/badge/Hibernate-6.4.4-59666C?logo=hibernate) | 6.4.4.Final | ORM / Implementação JPA |
| ![Flyway](https://img.shields.io/badge/Flyway-12.3.0-CC0200) | 12.3.0 | Migrações e versionamento do banco |
| SLF4J + Logback | 2.0.12 / 1.5.3 | Logging estruturado |
| JUnit 5 + H2 | — / 2.2.224 | Testes unitários e de integração |

---

## ✅ Funcionalidades

- **Gerenciamento de Tarefas** — criar, atualizar, concluir e reabrir tarefas com prioridades (`LOW`, `MEDIUM`, `HIGH`, `URGENT`)
- **Boards Kanban** — boards com colunas configuráveis e fluxo sequencial entre etapas
- **Movimentação de Cards** — progressão controlada entre colunas sem pular etapas
- **Bloqueio / Desbloqueio** — cards podem ser bloqueados com justificativa registrada
- **Histórico de Movimentações** — rastreamento completo de quando cada card passou por cada coluna
- **Relatórios** — tempo de conclusão e histórico de bloqueios por card
- **Migrações Automáticas** — Flyway para versionamento e evolução do schema do banco
- **Logging** — logs em console e arquivo com rotação diária via SLF4J/Logback

---

## 🔧 Como Instalar e Rodar

### Pré-requisitos

- [Java 21+](https://adoptium.net/)
- [MySQL 8.0+](https://dev.mysql.com/downloads/)
- [Gradle 9.2.0+](https://gradle.org/releases/) *(ou use o wrapper incluso)*

### 1. Clone o repositório

```bash
git clone https://github.com/AllanGiaretta26/task-management.git
cd task-management
```

### 2. Configure o banco de dados

Crie o banco de dados no MySQL:

```sql
CREATE DATABASE task_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Crie o arquivo de configuração a partir do template:

```bash
cp app/src/main/resources/database.properties.template app/src/main/resources/database.properties
```

Edite o arquivo `database.properties` com suas credenciais:

```properties
db.url=jdbc:mysql://localhost:3306/task_management
db.username=seu_usuario
db.password=sua_senha
```

> **⚠️ Importante:** O arquivo `database.properties` está no `.gitignore` — suas credenciais não serão versionadas.

### 3. Execute as migrações

```bash
./gradlew run --args="migrate"
```

### 4. Inicie a aplicação

```bash
./gradlew run
```

---

## 🧪 Testes

Execute todos os testes unitários e de integração:

```bash
./gradlew test
```

Build completo com geração do JAR:

```bash
./gradlew build
```

### Cobertura de testes

| Classe de Teste | Descrição |
|---|---|
| `TaskTest` | 14 testes para todas as operações da entidade `Task` |
| `AppTest` | 2 testes de validação da aplicação principal |
| `MigrationIntegrationTest` | 4 testes de migração e persistência JPA com banco H2 |

---

## 📁 Estrutura do Projeto

```
task-management/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/task/management/
│   │   │   │   ├── App.java                     # Ponto de entrada
│   │   │   │   ├── domain/                      # Entidades JPA
│   │   │   │   ├── service/                     # Regras de negócio
│   │   │   │   ├── infrastructure/              # Persistência e repositórios
│   │   │   │   ├── migrations/                  # Orquestração do Flyway
│   │   │   │   └── ui/                          # Interface CLI (menus)
│   │   │   └── resources/
│   │   │       ├── META-INF/persistence.xml     # Configuração JPA
│   │   │       ├── db/migration/                # Scripts SQL versionados
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

---

## 🏗️ Arquitetura

A aplicação segue uma arquitetura em camadas bem definida:

```
UI (CLI) → Service → Repository → JPA/Hibernate → MySQL
```

### Padrões utilizados

| Padrão | Aplicação |
|---|---|
| **Repository** | Encapsulamento do acesso a dados |
| **Service** | Encapsulamento das regras de negócio |
| **Factory** | Criação de serviços com configuração centralizada |
| **Entity** | Entidades JPA com validações e comportamento próprio |

---

## 🗄️ Banco de Dados

As migrações ficam em `app/src/main/resources/db/migration/` e são aplicadas automaticamente pelo Flyway.

| Migração | Tabelas criadas |
|---|---|
| `V1__create_tasks_table.sql` | `tasks` — id, title, description, completed, priority, timestamps |
| `V2__create_board_tables.sql` | `boards`, `columns`, `cards`, `blockades`, `column_history` |

---

## 📝 Configuração de Banco de Dados

```properties
db.url=jdbc:mysql://localhost:3306/task_management
db.username=
db.password=
```

---

## 📖 Documentação Adicional

- [Migrações de banco de dados](docs/annotation/migrations.md)
- [Configuração do Flyway](docs/annotation/flyway.md)
- [Implementação do Board](docs/annotation/board.md)
- [Recomendações e boas práticas](docs/annotation/recomendacoes.md)

---

## 🤝 Como Contribuir

1. Faça um fork do repositório
2. Crie uma branch para sua feature: `git checkout -b feature/minha-feature`
3. Commit suas alterações: `git commit -m 'feat: adiciona minha feature'`
4. Envie para a branch: `git push origin feature/minha-feature`
5. Abra um Pull Request

---

## 👤 Autor

**Allan Giaretta**

---

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).
