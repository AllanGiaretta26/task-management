# Task Management System

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.2-02303A?logo=gradle&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![License](https://img.shields.io/badge/licença-MIT-blue)

> Aplicação CLI de gerenciamento de tarefas e boards Kanban, com fluxo sequencial de cards, bloqueios com justificativa e histórico completo de movimentações.

---

## 📋 Descrição

O **Task Management System** é uma aplicação de linha de comando que une um gerenciador de tarefas com prioridades a um sistema de boards no estilo Kanban. Cards percorrem colunas em ordem sequencial, podem ser bloqueados com justificativa registrada, e todo o histórico de movimentação é rastreado para geração de relatórios.

O projeto foi desenvolvido para demonstrar boas práticas de arquitetura em camadas, uso de ORM com Hibernate JPA, versionamento de banco de dados com Flyway e testes automatizados com JUnit 5.

---

## 🚦 Status do Projeto

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

Funcionalidades principais implementadas e cobertas por testes. Projeto em evolução ativa.

---

## 🛠️ Tecnologias

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.2-02303A?logo=gradle&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-6.4.4-59666C?logo=hibernate&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-12.3.0-CC0200)
![JUnit5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)

- **Java 21** — linguagem principal
- **Gradle 9.2** — build e gerenciamento de dependências
- **MySQL 8.0+** — banco de dados relacional
- **Hibernate JPA 6.4.4** — ORM e persistência
- **Flyway 12.3.0** — migrações e versionamento do schema
- **SLF4J + Logback 2.0 / 1.5** — logging em console e arquivo
- **JUnit 5 + H2 2.2.224** — testes unitários e de integração

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

> **⚠️ Atenção:** `database.properties` está no `.gitignore` — suas credenciais não serão versionadas.

### 4. Execute as migrações

```bash
./gradlew run --args="migrate"
```

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

| Classe de teste | Descrição |
|---|---|
| `TaskTest` | 14 testes — operações da entidade `Task` |
| `AppTest` | 2 testes — validação da aplicação |
| `MigrationIntegrationTest` | 4 testes — migrações e persistência JPA com H2 |

---

## 🤝 Como Contribuir

1. Faça um fork do repositório
2. Crie uma branch: `git checkout -b feature/minha-feature`
3. Faça o commit: `git commit -m 'feat: descrição da mudança'`
4. Envie para a branch: `git push origin feature/minha-feature`
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).

---

Desenvolvido por Allan Giaretta.
