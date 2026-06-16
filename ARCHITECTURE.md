# Architecture — Task Management System

---

## Table of Contents

1. [Overview](#1-overview)
2. [Domain Layer](#2-domain-layer)
3. [Infrastructure Layer](#3-infrastructure-layer)
   - [3.1 JPA Utilities](#31-jpa-utilities)
   - [3.2 Configuration](#32-configuration)
   - [3.3 Repositories](#33-repositories)
4. [Service Layer](#4-service-layer)
5. [UI Layer](#5-ui-layer)
6. [Migration Subsystem](#6-migration-subsystem)
7. [Transaction Model](#7-transaction-model)
8. [Lazy-Loading Strategy](#8-lazy-loading-strategy)
9. [Schema Version History](#9-schema-version-history)
10. [Invariants and Constraints](#10-invariants-and-constraints)

---

## 1. Overview

Java 21 CLI application implementing a Kanban board with sequential card flow, blockade tracking, and time-based reporting. The persistence stack uses Hibernate 6.4.4 (ORM) over MySQL 8, with Flyway 12.3 as the exclusive schema authority (`hbm2ddl.auto=validate`). H2 in-memory is used exclusively in the test persistence unit.

**Package structure:**

```
task.management
├── App.java                        # Entry point; dispatches migrate vs. interactive
├── domain/                         # JPA entities and enums
├── infrastructure/
│   ├── config/                     # DatabaseConfig record + loader
│   ├── jpa/                        # JpaUtil (EMF lifecycle) + TransactionTemplate
│   └── repository/                 # Per-entity JPA repositories
├── migrations/                     # Flyway runner and factory
├── service/                        # Business logic; one class per aggregate root
└── ui/                             # Scanner-based CLI menu
```

**Layer dependency graph (strict, no upward references):**

```
UI  ──►  Service  ──►  Repository  ──►  JPA / Hibernate  ──►  MySQL
                   ──►  Domain
```

---

## 2. Domain Layer

**Location:** [domain/](app/src/main/java/task/management/domain/)

JPA entities mapped to the schema produced by Flyway. All relationships are bidirectional; owner side holds the FK column.

### Entity relationship

```
Board (1) ──< BoardColumn (N) ──< Card (N) ──┬──< Blockade (N)
                                              └──< ColumnHistory (N)
```

### Entity reference

| Class | Table | Key mappings |
|---|---|---|
| [Board](app/src/main/java/task/management/domain/Board.java) | `boards` | `@OneToMany(cascade=ALL, orphanRemoval=true)` → `BoardColumn`; `@OrderBy("position ASC")` |
| [BoardColumn](app/src/main/java/task/management/domain/BoardColumn.java) | `board_columns` | `@ManyToOne(fetch=LAZY)` → `Board`; `@OneToMany(cascade=ALL)` → `Card` |
| [Card](app/src/main/java/task/management/domain/Card.java) | `cards` | `@ManyToOne(fetch=LAZY)` → `BoardColumn`; `@OneToMany(cascade=ALL, orphanRemoval=true)` → `Blockade`, `ColumnHistory` |
| [Blockade](app/src/main/java/task/management/domain/Blockade.java) | `blockades` | `@ManyToOne(fetch=LAZY)` → `Card`; `isBlocking` flag differentiates block/unblock records |
| [ColumnHistory](app/src/main/java/task/management/domain/ColumnHistory.java) | `column_history` | `@ManyToOne(fetch=LAZY)` → `Card`, `BoardColumn`; `enteredAt` used for time calculations |
| [ColumnType](app/src/main/java/task/management/domain/ColumnType.java) | — | `INITIAL`, `PENDING`, `FINAL`, `CANCELLED`; `@Enumerated(STRING)` with `VARCHAR(20)` |
| [CardStatus](app/src/main/java/task/management/domain/CardStatus.java) | — | `ACTIVE`, `BLOCKED`; same strategy |

### Domain invariants enforced in-model

`Board.hasMinimumStructure()` — must have exactly one `INITIAL`, one `FINAL`, one `CANCELLED`, and at least three total columns.

`Board.getNextColumn(BoardColumn current)` — advances through `position ASC`, skipping `CANCELLED`; throws `IllegalArgumentException` on unknown column reference.

`Card.block(reason)` / `Card.unblock(reason)` — sets `CardStatus` and appends a `Blockade` record (`isBlocking=true/false`) inline; duration is computed by `Blockade.getDurationHours()` against the card's own blockade list.

`Card.getCompletionTimeHours()` — returns `Optional<Double>` from first `ColumnHistory.enteredAt` to the entry of the `FINAL` column; empty if not yet completed.

`ColumnHistory.getDurationInColumnHours()` — from `enteredAt` to the next history entry's timestamp, or `LocalDateTime.now()` for the current column.

---

## 3. Infrastructure Layer

### 3.1 JPA Utilities

#### [JpaUtil](app/src/main/java/task/management/infrastructure/jpa/JpaUtil.java)

Singleton `EntityManagerFactory`. Lazy-initialized on first call to `getEntityManagerFactory()`; closed explicitly via `JpaUtil.close()` in `App.main` finally block.

Runtime properties loaded from `DatabaseConfig` override the static values in `persistence.xml`, allowing the same XML to declare the production persistence unit while tests use `test-pu` with H2 and `hbm2ddl.auto=create-drop`.

```java
EntityManagerFactory emf = JpaUtil.getEntityManagerFactory();
EntityManager em = emf.createEntityManager();   // caller-managed lifecycle
```

#### [TransactionTemplate](app/src/main/java/task/management/infrastructure/jpa/TransactionTemplate.java)

Encapsulates `begin / commit / rollback` with three overloads:

```java
<T> T execute(Supplier<T> action)
void executeVoid(Runnable action)
<T> T execute(Supplier<T> action, Function<Exception, RuntimeException> exceptionWrapper)
```

The third overload is used by service methods to translate JPA exceptions into domain-specific `*ServiceException` at the boundary. On any `RuntimeException` the active transaction is rolled back before re-throwing.

### 3.2 Configuration

#### [DatabaseConfig](app/src/main/java/task/management/infrastructure/config/DatabaseConfig.java)

Immutable record: `{ String url, String username, String password }`.

#### [DatabaseConfigLoader](app/src/main/java/task/management/infrastructure/config/DatabaseConfigLoader.java)

Reads `database.properties` from the classpath root. Validates that `db.url`, `db.username`, and `db.password` are all present; throws `DatabaseConfigException` otherwise. Shared by `JpaUtil` and `MigrationRunnerFactory`, eliminating prior duplication.

`database.properties` is excluded from version control via `.gitignore`; `database.properties.template` provides the expected key set without credentials.

### 3.3 Repositories

**Location:** [infrastructure/repository/](app/src/main/java/task/management/infrastructure/repository/)

Each repository receives an `EntityManager` in its constructor and is scoped to a single operation (instantiated and discarded per service method call).

| Repository | Aggregate | Notable methods |
|---|---|---|
| [BoardRepository](app/src/main/java/task/management/infrastructure/repository/BoardRepository.java) | `Board` | `save`, `findById`, `findAll`, `findByName`, `delete`, `deleteById`, `initializeGraph(Board)`, `initializeGraph(List<Board>)` |
| [CardRepository](app/src/main/java/task/management/infrastructure/repository/CardRepository.java) | `Card` | `save`, `findById`, `findByColumnId`, `findByBoardId`, `findByColumnIdAndStatus`, `delete` |
| [ColumnRepository](app/src/main/java/task/management/infrastructure/repository/ColumnRepository.java) | `BoardColumn` | `save`, `findById`, `findByBoardId`, `findByBoardIdAndType`, `delete` |
| [BlockadeRepository](app/src/main/java/task/management/infrastructure/repository/BlockadeRepository.java) | `Blockade` | `save`, `findByCard`, `findByCardAndPeriod` |

`BoardRepository.initializeGraph` forces full traversal of the object graph (`Board → columns → cards → blockades, columnHistory`) by iterating collections within the open `EntityManager`. This deliberate N+1 pattern is acceptable for a single-user CLI; it avoids `MultipleBagFetchException` that would arise from multi-collection FETCH JOIN queries on the same root entity.

---

## 4. Service Layer

**Location:** [service/](app/src/main/java/task/management/service/)

All services receive `EntityManagerFactory` at construction and apply the **one-`EntityManager`-per-operation** pattern:

```java
public Board createBoard(String name, ...) {
    try (EntityManager em = emf.createEntityManager()) {
        BoardRepository repo = new BoardRepository(em);
        TransactionTemplate tx = new TransactionTemplate(em);

        Board saved = tx.execute(
            () -> repo.save(board),
            e  -> new BoardServiceException("...", e)
        );

        repo.initializeGraph(saved);   // inflate lazy collections before EM closes
        return saved;                  // returned entity is detached
    }
}
```

The `EntityManager` is opened inside `try-with-resources`, ensuring it closes on every exit path. The entity returned to the caller is always **detached** with all navigable associations pre-loaded.

### BoardService

| Method | Behaviour |
|---|---|
| `createBoard(name, initial, final, cancelled)` | Persists board with three mandatory columns (`INITIAL`, `FINAL`, `CANCELLED`) at positions 0, 1, 2 |
| `addPendingColumn(boardId, name)` | Inserts a `PENDING` column before the `FINAL` column, shifting subsequent positions |
| `listAllBoards()` | Returns all boards with inflated graphs |
| `findBoardById(id)` | Returns single board with inflated graph; throws `BoardServiceException` if absent |
| `deleteBoard(id)` | Cascades through JPA (`orphanRemoval`) |

### CardService

| Method | Validation |
|---|---|
| `createCard(boardId, title, desc)` | Card enters `INITIAL` column; `ColumnHistory` entry recorded |
| `moveCard(cardId, targetColumnId)` | Rejects blocked cards; `null` target advances to next sequential column |
| `cancelCard(cardId)` | Rejects blocked cards and cards already in `FINAL` |
| `blockCard(cardId, reason)` | Appends `Blockade(isBlocking=true)` to card |
| `unblockCard(cardId, reason)` | Appends `Blockade(isBlocking=false)` to card |
| `listCardsByBoard(boardId)` | Returns cards ordered by `createdAt` |
| `findCardById(id)` | Throws `CardServiceException` if absent |

Internal `executeInTransaction(CardOperation, errorPrefix)` collapses the repetitive EM-open/tx/initializeGraph sequence for card operations.

### ReportService

Read-only; opens `EntityManager` without `TransactionTemplate`. Generates formatted text reports:

- `generateCompletionTimeReport(boardId)` — per-card time in each column; `ColumnHistory.getDurationInColumnHours()` per entry.
- `generateBlockadeReport(boardId)` — per-card blockade list with durations; `Blockade.getDurationHours()` per record.

Locale: `pt-BR`. Date format: `dd/MM/yyyy HH:mm`.

### Exception types

All extend `RuntimeException`. Service layer wraps JPA exceptions at the transaction boundary; UI layer catches and renders messages.

```
RuntimeException
├── BoardServiceException
├── CardServiceException
├── ReportServiceException
├── DatabaseConfigException      (nested in DatabaseConfigLoader)
└── MigrationService.MigrationException
```

---

## 5. UI Layer

**Location:** [ui/](app/src/main/java/task/management/ui/)

`BoardMenu` is a `Scanner`-driven loop with no persistence calls. It instantiates the three services from the shared `EntityManagerFactory` and dispatches to service methods based on integer input. All `*ServiceException` are caught and printed without stack traces; unexpected `RuntimeException` is logged and propagates to `App.main`.

---

## 6. Migration Subsystem

**Location:** [migrations/](app/src/main/java/task/management/migrations/)

Invoked when `App.main` receives `"migrate"` as `args[0]`.

```
App.main("migrate")
  └── MigrationExecutor.run()
        └── MigrationRunnerFactory.createMigrationService()   // configures Flyway
              └── MigrationService.runMigrations()            // flyway.migrate()
```

`MigrationRunnerFactory` loads `DatabaseConfig` via `DatabaseConfigLoader` and builds the Flyway instance targeting the `db/migration` classpath location. `MigrationService.runMigrations()` wraps `FlywayException` in `MigrationException`.

---

## 7. Transaction Model

- **Write operations**: `TransactionTemplate.execute(...)` — explicit `begin`, `commit` on success, `rollback` on `RuntimeException`.
- **Read operations** (`ReportService`): no transaction; `EntityManager` is opened, queries executed, closed.
- **Isolation**: Each service method owns its `EntityManager` lifetime. No shared EM exists across calls, eliminating first-level cache growth across the CLI session and preventing accidental cross-operation state bleed.

---

## 8. Lazy-Loading Strategy

All `@OneToMany` and `@ManyToOne` associations use `fetch=LAZY` (default). Collections are inflated by `initializeGraph` before the `EntityManager` closes:

```java
// BoardRepository.initializeGraph (simplified)
board.getColumns().forEach(col -> {
    col.getCards().forEach(card -> {
        card.getBlockades().size();
        card.getColumnHistory().size();
    });
});
```

Returned entities are detached. Any access to an un-initialized association post-detach would raise `LazyInitializationException`. Service methods are responsible for calling `initializeGraph` before returning; this must be repeated in any new service method that returns entity objects.

---

## 9. Schema Version History

**Location:** [db/migration/](app/src/main/resources/db/migration/)

| Version | File | Summary |
|---|---|---|
| V1 | `V1__create_tasks_table.sql` | Legacy `tasks` table (superseded) |
| V2 | `V2__create_board_tables.sql` | `boards`, `board_columns`, `cards`, `blockades`, `column_history` |
| V3 | `V3__cleanup_legacy_schema.sql` | Drops `tasks`; renames `columns` → `board_columns` to avoid SQL reserved word collision; standardizes enum columns to `VARCHAR(20)` |

**Persistence unit settings:**

| Unit | `hbm2ddl.auto` | Driver | Purpose |
|---|---|---|---|
| `task-management-pu` | `validate` | `com.mysql.cj.jdbc.Driver` | Production |
| `test-pu` | `create-drop` | `org.h2.Driver` | Integration tests |

Hibernate schema validation runs at `EntityManagerFactory` creation. Mismatch between entity mappings and the current schema causes startup failure.

---

## 10. Invariants and Constraints

**Column ordering:** `PENDING` columns sort between `INITIAL` (position 0) and `FINAL`. `addPendingColumn` shifts `FINAL` and `CANCELLED` positions to maintain contiguity. `getNextColumn` skips `CANCELLED` in the sequence.

**Card transitions:** Sequential only — a card may not move from `INITIAL` directly to `FINAL`. Move is rejected if the card's `CardStatus` is `BLOCKED`. Cancel is rejected if the card is in `FINAL`.

**Enum persistence:** `@Enumerated(EnumType.STRING)` on all enum fields. Using `ORDINAL` would silently corrupt data on enum reordering.

**Reserved word avoidance:** Table `board_columns` avoids the SQL reserved word `columns`. When adding tables, avoid `order`, `group`, `user`, `select`, and `columns`.

**Credential handling:** `database.properties` is `.gitignore`-listed. The `database.properties.template` file documents required keys; it must never contain real credentials.

**Connection pool:** Production persistence unit configures `hibernate.connection.pool_size=5`. For concurrent workloads a dedicated pool (e.g., HikariCP) would be required; the current setup is appropriate for a single-threaded CLI.
