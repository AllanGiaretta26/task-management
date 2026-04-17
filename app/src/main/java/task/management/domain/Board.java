package task.management.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidade que representa um board no sistema de gerenciamento de tarefas.
 *
 * Um board é composto por um nome e uma coleção ordenada de colunas.
 *
 * @author Allan Giaretta
 * @version 2.0
 */
@Entity
@Table(name = "boards")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<BoardColumn> columns = new ArrayList<>();

    /**
     * Construtor padrão requerido pelo JPA.
     */
    public Board() {
    }

    /**
     * Construtor com nome do board.
     *
     * @param name Nome do board
     */
    public Board(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Adiciona uma coluna ao board.
     *
     * @param column Coluna a ser adicionada
     */
    public void addColumn(BoardColumn column) {
        columns.add(column);
        column.setBoard(this);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Remove uma coluna do board.
     *
     * @param column Coluna a ser removida
     */
    public void removeColumn(BoardColumn column) {
        columns.remove(column);
        column.setBoard(null);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Retorna uma lista não modificável das colunas ordenadas por posição.
     *
     * @return Lista de colunas
     */
    public List<BoardColumn> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Encontra uma coluna pelo seu ID.
     *
     * @param columnId ID da coluna
     * @return Coluna encontrada ou null
     */
    public BoardColumn findColumnById(Long columnId) {
        return columns.stream()
                .filter(c -> c.getId() != null && c.getId().equals(columnId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Encontra uma coluna pelo seu tipo.
     *
     * @param type Tipo da coluna
     * @return Coluna encontrada ou null
     */
    public BoardColumn findColumnByType(ColumnType type) {
        return columns.stream()
                .filter(c -> c.getType() == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * Valida se o board possui a estrutura mínima de colunas.
     *
     * Um board deve ter pelo menos 3 colunas: inicial, final e cancelada.
     *
     * @return true se válido, false caso contrário
     */
    public boolean hasMinimumStructure() {
        boolean hasInitial = columns.stream().anyMatch(c -> c.getType() == ColumnType.INITIAL);
        boolean hasFinal = columns.stream().anyMatch(c -> c.getType() == ColumnType.FINAL);
        boolean hasCancelled = columns.stream().anyMatch(c -> c.getType() == ColumnType.CANCELLED);
        return hasInitial && hasFinal && hasCancelled && columns.size() >= 3;
    }

    /**
     * Retorna a próxima coluna no fluxo para uma determinada coluna.
     * Não inclui a coluna de cancelamento no fluxo normal.
     *
     * @param currentColumn Coluna atual
     * @return Próxima coluna ou null se não houver
     * @throws IllegalArgumentException se currentColumn não pertencer a este board
     */
    public BoardColumn getNextColumn(BoardColumn currentColumn) {
        if (currentColumn == null) {
            throw new IllegalArgumentException("currentColumn não pode ser null");
        }

        int currentIndex = -1;
        for (int i = 0; i < columns.size(); i++) {
            BoardColumn c = columns.get(i);
            if (c == currentColumn ||
                (c.getId() != null && c.getId().equals(currentColumn.getId()))) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new IllegalArgumentException(
                    "Coluna informada não pertence a este board: " + currentColumn);
        }

        // Não inclui coluna de cancelamento no fluxo normal
        for (int i = currentIndex + 1; i < columns.size(); i++) {
            BoardColumn next = columns.get(i);
            if (next.getType() != ColumnType.CANCELLED) {
                return next;
            }
        }
        return null;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        if (id == null || board.id == null) {
            return false;
        }
        return Objects.equals(id, board.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Board{id=%d, name='%s', columns=%d}",
                id, name, columns.size());
    }
}
