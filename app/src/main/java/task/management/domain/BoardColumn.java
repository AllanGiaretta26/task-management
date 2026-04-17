package task.management.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidade que representa uma coluna em um board.
 *
 * Cada coluna possui um nome, tipo, posição no board e uma coleção de cards.
 *
 * @author Allan Giaretta
 * @version 2.0
 */
@Entity
@Table(name = "board_columns", indexes = {
    @Index(name = "idx_column_board_position", columnList = "board_id, position")
})
public class BoardColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private ColumnType type;

    @Column(nullable = false)
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @OneToMany(mappedBy = "column", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Card> cards = new ArrayList<>();

    /**
     * Construtor padrão requerido pelo JPA.
     */
    public BoardColumn() {
    }

    /**
     * Construtor com nome, tipo e posição.
     *
     * @param name Nome da coluna
     * @param type Tipo da coluna
     * @param position Posição no board
     */
    public BoardColumn(String name, ColumnType type, Integer position) {
        this.name = name;
        this.type = type;
        this.position = position;
    }

    /**
     * Adiciona um card à coluna.
     *
     * @param card Card a ser adicionado
     */
    public void addCard(Card card) {
        cards.add(card);
        card.setColumn(this);
    }

    /**
     * Remove um card da coluna.
     *
     * Simétrico a {@link #addCard(Card)}: limpa a referência bidirecional.
     * Após esta chamada, o card fica sem coluna associada — é responsabilidade
     * do chamador adicioná-lo a outra coluna antes de persistir, já que a
     * relação com coluna é obrigatória (nullable = false).
     *
     * @param card Card a ser removido
     */
    public void removeCard(Card card) {
        cards.remove(card);
        card.setColumn(null);
    }

    /**
     * Retorna uma lista não modificável dos cards.
     *
     * @return Lista de cards
     */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /**
     * Encontra um card pelo seu ID.
     *
     * @param cardId ID do card
     * @return Card encontrado ou null
     */
    public Card findCardById(Long cardId) {
        return cards.stream()
                .filter(c -> c.getId() != null && c.getId().equals(cardId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifica se esta é a coluna inicial.
     *
     * @return true se for coluna inicial
     */
    public boolean isInitial() {
        return type == ColumnType.INITIAL;
    }

    /**
     * Verifica se esta é a coluna final.
     *
     * @return true se for coluna final
     */
    public boolean isFinal() {
        return type == ColumnType.FINAL;
    }

    /**
     * Verifica se esta é a coluna de cancelamento.
     *
     * @return true se for coluna de cancelamento
     */
    public boolean isCancelled() {
        return type == ColumnType.CANCELLED;
    }

    /**
     * Verifica se esta é uma coluna pendente.
     *
     * @return true se for coluna pendente
     */
    public boolean isPending() {
        return type == ColumnType.PENDING;
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
    }

    public ColumnType getType() {
        return type;
    }

    public void setType(ColumnType type) {
        this.type = type;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardColumn column = (BoardColumn) o;
        // Se ambos têm ID, compara. Se um não tem ID, não são iguais.
        if (id == null || column.id == null) {
            return false;
        }
        return Objects.equals(id, column.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Column{id=%d, name='%s', type=%s, position=%d, cards=%d}",
                id, name, type, position, cards.size());
    }
}
