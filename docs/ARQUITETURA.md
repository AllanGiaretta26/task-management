# Documentação Técnica — Task Management System

> Guia de leitura do código-fonte para iniciantes em Java. Explica **o que cada parte faz**, **como se conectam**, **como usar** e **onde as pessoas costumam tropeçar**.

---

## Sumário

1. [Visão geral](#1-visão-geral)
2. [Explicação por seção](#2-explicação-por-seção)
   - [2.1 Camada de domínio](#21-camada-de-domínio)
   - [2.2 Camada de infraestrutura](#22-camada-de-infraestrutura)
   - [2.3 Camada de repositório](#23-camada-de-repositório)
   - [2.4 Camada de serviço](#24-camada-de-serviço)
   - [2.5 Camada de UI](#25-camada-de-ui)
   - [2.6 Migrações Flyway](#26-migrações-flyway)
3. [Exemplo de uso](#3-exemplo-de-uso)
4. [Pontos de atenção](#4-pontos-de-atenção)

---

## 1. Visão geral

O **Task Management System** é uma aplicação **CLI em Java 21** que gerencia quadros Kanban. O usuário cria *boards*, adiciona colunas, move *cards* entre elas, bloqueia/desbloqueia cards com justificativa e gera relatórios de tempo e bloqueio.

O código está organizado em **quatro camadas bem separadas**, cada uma com uma única responsabilidade:

```
┌──────────────────────────────────────────────────────┐
│  UI        (BoardMenu)    ← interação com o usuário  │
│  Service   (BoardService, CardService, ReportService)│
│  Repository (BoardRepository, CardRepository)        │
│  JPA/DB    (JpaUtil, TransactionTemplate, MySQL)     │
└──────────────────────────────────────────────────────┘
```

**Fluxo típico:** `App.main()` → inicializa JPA → mostra `BoardMenu` → usuário escolhe opção → menu chama um `Service` → o service abre um `EntityManager`, usa um `Repository` dentro de uma transação, carrega o grafo de objetos e devolve a entidade para a UI exibir.

Tecnologias envolvidas: **Hibernate/JPA** (ORM), **Flyway** (versionamento de schema), **MySQL** (banco), **H2** (apenas nos testes).

---

## 2. Explicação por seção

### 2.1 Camada de domínio

Localização: [app/src/main/java/task/management/domain/](../app/src/main/java/task/management/domain/)

São as **entidades JPA** — classes Java que viram tabelas no banco. Cada uma tem anotações (`@Entity`, `@Table`, `@OneToMany`, etc.) que ensinam o Hibernate a mapeá-las.

#### Diagrama de relações

```
Board (1) ──< BoardColumn (N) ──< Card (N) ──┬──< Blockade (N)
                                              └──< ColumnHistory (N)
```

| Classe | Responsabilidade | Relações principais |
|---|---|---|
| [Board](../app/src/main/java/task/management/domain/Board.java) | Quadro Kanban. Agrupa colunas e valida que existe pelo menos **INITIAL**, **FINAL** e **CANCELLED**. | `@OneToMany` para `BoardColumn` (cascade ALL + orphanRemoval) |
| [BoardColumn](../app/src/main/java/task/management/domain/BoardColumn.java) | Uma coluna do board com `type`, `position` e lista de cards. Tabela: `board_columns`. | `@ManyToOne` para `Board`; `@OneToMany` para `Card` |
| [Card](../app/src/main/java/task/management/domain/Card.java) | Tarefa. Guarda bloqueios, histórico de colunas e calcula métricas (tempo bloqueado, tempo até conclusão). | `@ManyToOne` para `BoardColumn`; `@OneToMany` para `Blockade` e `ColumnHistory` |
| [Blockade](../app/src/main/java/task/management/domain/Blockade.java) | Registro de um bloqueio ou desbloqueio com motivo e timestamp. | `@ManyToOne` para `Card` |
| [ColumnHistory](../app/src/main/java/task/management/domain/ColumnHistory.java) | Registra **quando** um card entrou em **qual** coluna. Usado para os relatórios. | `@ManyToOne` para `Card` e para `BoardColumn` |
| [ColumnType](../app/src/main/java/task/management/domain/ColumnType.java) | Enum: `INITIAL`, `PENDING`, `FINAL`, `CANCELLED`. Persistido como `VARCHAR(20)`. | — |
| [CardStatus](../app/src/main/java/task/management/domain/CardStatus.java) | Enum: `ACTIVE`, `BLOCKED`. | — |

**Para iniciantes:** pense nessas classes como *formulários em papel*. O Hibernate é quem lê as anotações, gera o SQL e copia os dados entre objeto Java e linha da tabela. Você nunca escreve `INSERT` na mão.

---

### 2.2 Camada de infraestrutura

Localização: [app/src/main/java/task/management/infrastructure/](../app/src/main/java/task/management/infrastructure/)

Classes que **não têm regra de negócio** — elas só existem para configurar JPA, transações e credenciais.

#### [JpaUtil](../app/src/main/java/task/management/infrastructure/jpa/JpaUtil.java)

O "gerente de conexões". Cria **um único** `EntityManagerFactory` (o caro de criar) e entrega `EntityManager` novos sob demanda (baratos).

```java
EntityManagerFactory emf = JpaUtil.getEntityManagerFactory();
EntityManager em = emf.createEntityManager();   // uso único, descartável
```

#### [TransactionTemplate](../app/src/main/java/task/management/infrastructure/jpa/TransactionTemplate.java)

Encapsula o padrão `begin → operação → commit` (ou `rollback` em caso de erro). Sem ele, todo service teria 15 linhas repetidas de *try/catch* ao redor de cada operação.

#### [DatabaseConfig](../app/src/main/java/task/management/infrastructure/config/) + DatabaseConfigLoader

Um **record** imutável com url/usuário/senha, carregado de `database.properties`. Centraliza a leitura para não duplicar código em `JpaUtil` e `MigrationRunnerFactory`.

#### [persistence.xml](../app/src/main/resources/META-INF/persistence.xml)

Arquivo XML que lista as entidades JPA e define propriedades do Hibernate (dialeto, `hbm2ddl.auto=validate`, pool de conexões).

---

### 2.3 Camada de repositório

Localização: [app/src/main/java/task/management/infrastructure/repository/](../app/src/main/java/task/management/infrastructure/repository/)

"CRUD sobre a entidade". Recebe um `EntityManager` e expõe `save`, `findById`, `findAll`, `delete`.

Destaque em [BoardRepository](../app/src/main/java/task/management/infrastructure/repository/BoardRepository.java): o método `initializeGraph(board)` **força** o carregamento das coleções *lazy* (colunas → cards → bloqueios → histórico) iterando sobre elas dentro da transação. Sem isso, ao tentar ler `board.getColumns()` na UI (depois do `EntityManager` fechar), ocorre `LazyInitializationException`.

---

### 2.4 Camada de serviço

Localização: [app/src/main/java/task/management/service/](../app/src/main/java/task/management/service/)

É onde mora a **regra de negócio**. Três services, todos seguindo o **mesmo padrão** (*EntityManager per-operation*):

```java
public Board createBoard(String name, ...) {
    try (EntityManager em = emf.createEntityManager()) {   // 1. abre EM
        BoardRepository repo = new BoardRepository(em);
        TransactionTemplate tx = new TransactionTemplate(em);

        Board saved = tx.execute(                          // 2. transação
            () -> repo.save(board),
            e  -> new BoardServiceException("Erro: " + e.getMessage(), e)
        );

        repo.initializeGraph(saved);                       // 3. infla lazy
        return saved;                                      // 4. detached
    }                                                      // 5. EM fecha sozinho
}
```

| Service | Métodos principais | O que faz |
|---|---|---|
| [BoardService](../app/src/main/java/task/management/service/BoardService.java) | `createBoard`, `addPendingColumn`, `listAllBoards`, `findBoardById`, `deleteBoard` | Cria quadros com as 3 colunas obrigatórias; insere colunas `PENDING` entre inicial e final, reordenando posições |
| [CardService](../app/src/main/java/task/management/service/CardService.java) | `createCard`, `moveCard`, `cancelCard`, `blockCard`, `unblockCard`, `listCardsByBoard`, `findCardById` | Ciclo de vida dos cards. Valida: não mover bloqueado, não pular colunas, não cancelar card já finalizado |
| [ReportService](../app/src/main/java/task/management/service/ReportService.java) | `generateCompletionTimeReport`, `generateBlockadeReport` | Gera relatórios em texto (**read-only**, sem `TransactionTemplate`) |

**Exceções dedicadas:** `BoardServiceException`, `CardServiceException`, `ReportServiceException` — todas são `RuntimeException`.

---

### 2.5 Camada de UI

Localização: [app/src/main/java/task/management/ui/](../app/src/main/java/task/management/ui/) e [App.java](../app/src/main/java/task/management/App.java)

#### [App.java](../app/src/main/java/task/management/App.java)

Ponto de entrada. Decide:
- Se receber `"migrate"` como argumento → chama [MigrationExecutor](../app/src/main/java/task/management/migrations/MigrationExecutor.java) e encerra.
- Caso contrário → abre o `BoardMenu`.

No final garante `JpaUtil.close()` para liberar o pool.

#### [BoardMenu.java](../app/src/main/java/task/management/ui/BoardMenu.java)

Menu interativo em `Scanner`. Obtém o `EntityManagerFactory` de `JpaUtil`, instancia os três services e chama o método correspondente à escolha do usuário. **Nenhuma consulta ao banco sai daqui** — a UI só traduz texto ↔ chamada de service.

---

### 2.6 Migrações Flyway

Localização: [app/src/main/resources/db/migration/](../app/src/main/resources/db/migration/)

Flyway executa em ordem, uma única vez cada:

| Versão | Conteúdo |
|---|---|
| **V1** | Estrutura inicial (tabela legada `tasks`) |
| **V2** | Cria `boards`, `columns`, `cards`, `blockades`, `column_history` |
| **V3** | Remove `tasks`; renomeia `columns` → `board_columns` (evita palavra reservada do SQL); padroniza colunas de enum como `VARCHAR(20)` |

O [MigrationRunnerFactory](../app/src/main/java/task/management/migrations/MigrationRunnerFactory.java) configura Flyway com as credenciais de `DatabaseConfig` e o [MigrationExecutor](../app/src/main/java/task/management/migrations/MigrationExecutor.java) apenas dispara `migrate()`.

---

## 3. Exemplo de uso

Fluxo completo chamando os serviços diretamente (sem passar pelo menu):

```java
import jakarta.persistence.EntityManagerFactory;
import task.management.domain.Board;
import task.management.domain.Card;
import task.management.infrastructure.jpa.JpaUtil;
import task.management.service.BoardService;
import task.management.service.CardService;
import task.management.service.ReportService;

public class Exemplo {
    public static void main(String[] args) {
        EntityManagerFactory emf = JpaUtil.getEntityManagerFactory();

        BoardService boardService  = new BoardService(emf);
        CardService  cardService   = new CardService(emf);
        ReportService reportService = new ReportService(emf);

        // 1. Cria um board com as 3 colunas obrigatórias
        Board board = boardService.createBoard(
                "Sprint 01", "A Fazer", "Concluído", "Cancelado");

        // 2. Insere uma coluna intermediária
        boardService.addPendingColumn(board.getId(), "Em Progresso");

        // 3. Cria um card (nasce na coluna INITIAL)
        Card card = cardService.createCard(
                board.getId(), "Implementar login", "Tela + validação");

        // 4. Move para a próxima coluna (null = "avançar sequencialmente")
        cardService.moveCard(card.getId(), null);

        // 5. Bloqueia e desbloqueia com justificativa
        cardService.blockCard(card.getId(), "Aguardando aprovação");
        cardService.unblockCard(card.getId(), "Aprovado");

        // 6. Gera relatório
        String relatorio = reportService.generateCompletionTimeReport(board.getId());
        System.out.println(relatorio);

        JpaUtil.close();
    }
}
```

Saída resumida:

```
================================================================================
RELATÓRIO DE TEMPO DE CONCLUSÃO - SPRINT 01
================================================================================

Card: Implementar login
ID: 1  |  Status: ATIVO
Criado em: 17/04/2026 10:30
Tempo em cada coluna:
  - A Fazer       : 0.05 h
  - Em Progresso  : 0.01 h (atual)
...
```

---

## 4. Pontos de atenção

### ⚠️ `LazyInitializationException` — o erro mais comum com JPA

**Sintoma:** você lê `board.getColumns().size()` na UI e estoura uma exceção.

**Causa:** o `EntityManager` fechou (saiu do `try-with-resources`), mas a coleção ainda estava marcada para carregamento *lazy*.

**Como o projeto resolve:** cada service chama `repo.initializeGraph(entity)` **antes** de retornar. Se criar um service novo, lembre-se de fazer o mesmo.

### ⚠️ Não reutilize o `EntityManager` entre operações

Cada método de service abre o seu próprio e descarta. Isso é **intencional** — evita cache de 1º nível crescendo indefinidamente durante a sessão CLI. Nunca guarde um `EntityManager` como atributo de classe.

### ⚠️ Palavras reservadas no SQL

A tabela chamava-se `columns`, que é palavra reservada em vários bancos. A migração **V3** renomeou para `board_columns`. Ao criar novas tabelas, evite nomes como `order`, `group`, `user`, `select`.

### ⚠️ Enum persistido como string

As colunas `type` e `status` usam `@Enumerated(EnumType.STRING)` com `columnDefinition = "VARCHAR(20)"`. **Nunca** use `EnumType.ORDINAL` — se alguém reordenar o enum no código, o banco passa a apontar para o valor errado silenciosamente.

### ⚠️ Regras de negócio que estouram exceção

| Tentativa | Exceção |
|---|---|
| Mover card bloqueado | `CardServiceException` com "bloqueado" |
| Pular colunas (ex.: ir de INITIAL direto para FINAL) | `CardServiceException` com "pular" |
| Cancelar card já na coluna FINAL | `CardServiceException` |
| Criar board com nome vazio | `BoardServiceException` com "vazio" |
| Buscar board/card por ID inexistente | `*ServiceException` com "não encontrado" |

Sempre envolva chamadas de service em `try/catch` na UI para mostrar mensagens amigáveis.

### ⚠️ Banco precisa estar de pé

O código assume MySQL rodando em `localhost:3306`. Se o serviço estiver parado, você verá `java.net.ConnectException: Connection refused`. No Windows: `Start-Service MySQL94` (nome do serviço pode variar) ou abra *Services.msc*.

### ⚠️ `database.properties` nunca vai para o Git

O `.gitignore` protege esse arquivo — credenciais reais **nunca** devem ser commitadas. Use `database.properties.template` como referência e preencha localmente.

---

*Documento gerado para leitura em paralelo ao código. Se algum trecho divergir da implementação, o código é a fonte da verdade.*
