# Task Management System

![Status](https://img.shields.io/badge/status-concluído-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.2-02303A?logo=gradle&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![License](https://img.shields.io/badge/licença-MIT-blue)

> Aplicação CLI de boards Kanban com fluxo sequencial de cards, bloqueios com justificativa e histórico completo de movimentações.

---

## 📋 Descrição

O **Task Management System** é uma aplicação de linha de comando para gerenciamento de projetos no estilo Kanban. Cards percorrem colunas em ordem sequencial, podem ser bloqueados com justificativa registrada, e todo o histórico de movimentação é rastreado para geração de relatórios.

O projeto demonstra boas práticas de arquitetura em camadas (UI → Service → Repository → JPA), ORM com Hibernate, versionamento de banco de dados com Flyway e testes automatizados com JUnit 5, incluindo testes de integração contra banco H2 in-memory.

---

## 🚦 Status do Projeto

![Status](https://img.shields.io/badge/status-concluído-brightgreen)

Funcionalidades principais implementadas e cobertas por 65 testes (unitários + integração). Arquitetura finalizada.

---

## 🏗️ Arquitetura

A aplicação segue o padrão de camadas:

```
UI (BoardMenu)
  └── Service (BoardService, CardService, ReportService)
        └── Repository (BoardRepository, CardRepository)
              └── JPA / Hibernate → MySQL
```

**Decisões de design notáveis:**

- **EntityManager per-operation**: cada método de service abre e fecha seu próprio `EntityManager` via `try-with-resources`. Isso elimina acúmulo de cache de 1º nível durante a sessão CLI e o risco de `LazyInitializationException` — o grafo lazy (colunas → cards → bloqueios → histórico) é inflado antes do EM fechar, e a entidade é devolvida detached.
- **Flyway**: migrações versionadas garantem evolução controlada do schema (3 versões aplicadas).
- **Repositório**: separa o acesso a dados da lógica de negócio; torna os services testáveis isoladamente.
- **Modelo de domínio limpo**: a entidade legada `Task` foi removida — todo o fluxo de trabalho agora se apoia em `Card` + `BoardColumn` + `ColumnHistory` + `Blockade`, que refletem melhor o modelo Kanban.
- **Mapeamento de enums estável**: colunas `type` e `operation_type` usam `@Enumerated(EnumType.STRING)` com `columnDefinition = "VARCHAR(20)"`, evitando divergências entre o DDL gerado pelo Hibernate e o schema aplicado pelo Flyway.

---

## 🛠️ Tecnologias

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.2-02303A?logo=gradle&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-6.4.4-59666C?logo=hibernate&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-12.3.0-CC0200)
![JUnit5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Gradle | 9.2 | Build e dependências |
| MySQL | 8.0+ | Banco de dados relacional |
| Hibernate JPA | 6.4.4 | ORM e persistência |
| Flyway | 12.3.0 | Migrações e versionamento do schema |
| SLF4J + Logback | 2.0 / 1.5 | Logging |
| JUnit 5 + H2 | 5 / 2.2.224 | Testes unitários e de integração |

---

## 🔧 Como Instalar e Rodar

### Pré-requisitos

- [Java 21+](https://adoptium.net/)
- [MySQL 8.0+](https://dev.mysql.com/downloads/)
- Gradle 9.2+ *(ou use o wrapper `./gradlew` incluso)*

### 1. Clone o repositório

```bash
git clone https://github.com/AllanGiaretta26/task-management.git
cd task-management
```

### 2. Crie o banco de dados

```sql
CREATE DATABASE task_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure as credenciais

Copie o template e preencha com seus dados:

```bash
cp app/src/main/resources/database.properties.template \
   app/src/main/resources/database.properties
```

Edite `database.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/task_management
db.username=seu_usuario
db.password=sua_senha
```

> **⚠️ Atenção:** `database.properties` está no `.gitignore` — suas credenciais **nunca** serão versionadas. Nunca edite o arquivo `.template` com credenciais reais.

### 4. Execute as migrações

```bash
./gradlew run --args="migrate"
```

Isso aplica as 3 migrations Flyway e cria o schema completo:

| Versão | Descrição |
|---|---|
| V1 | Estrutura inicial |
| V2 | Boards, colunas, cards, bloqueios e histórico |
| V3 | Limpeza de legado: renomeia `columns` → `board_columns` (evita conflito com palavra reservada do SQL), remove a tabela legada `tasks` e padroniza colunas de enum como `VARCHAR(20)` |

### 5. Inicie a aplicação

```bash
./gradlew run
```

---

## 🧪 Como Testar

```bash
# Todos os testes
./gradlew test

# Build completo com geração do JAR
./gradlew build
```

Os testes usam banco H2 em memória — nenhuma configuração adicional necessária.

| Classe de teste | Testes | Tipo | Descrição |
|---|---|---|---|
| `AppTest` | 2 | Unitário | Validação da aplicação |
| `BoardTest` | 12 | Unitário | Operações de board e colunas |
| `BoardColumnTest` | 16 | Unitário | Comportamento de colunas |
| `CardTest` | 18 | Unitário | Ciclo de vida e histórico de cards |
| `MigrationIntegrationTest` | 1 | Integração | Criação do serviço de migração |
| `BoardServiceIT` | 6 | Integração | BoardService contra H2 (criar, buscar, excluir, colunas) |
| `CardServiceIT` | 6 | Integração | CardService contra H2 (criar, mover, cancelar, bloquear) |
| `ReportServiceIT` | 4 | Integração | ReportService contra H2 (relatórios de conclusão e bloqueio) |

> Os testes de integração (`*IT`) usam `AbstractJpaIT` como base: compartilham um `EntityManagerFactory` por classe e limpam as tabelas antes de cada teste respeitando a ordem de FK.

---

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).

---

Desenvolvido por [Allan Giaretta](https://github.com/AllanGiaretta26).
