package task.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade que registra o histórico de movimentação de um card entre colunas.
 *
 * Cada registro armazena quando o card entrou em uma coluna específica.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
@Entity
@Table(name = "column_history", indexes = {
    @Index(name = "idx_history_card", columnList = "card_id"),
    @Index(name = "idx_history_column", columnList = "column_id"),
    @Index(name = "idx_history_entered_at", columnList = "entered_at")
})
public class ColumnHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    /**
     * Construtor padrão requerido pelo JPA.
     */
    public ColumnHistory() {
    }

    /**
     * Construtor com card e coluna.
     *
     * @param card Card que entrou na coluna
     * @param column Coluna onde o card entrou
     */
    public ColumnHistory(Card card, BoardColumn column) {
        this.card = card;
        this.column = column;
        this.enteredAt = LocalDateTime.now();
    }

    /**
     * Calcula o tempo que o card ficou nesta coluna.
     *
     * @return Tempo em horas
     */
    public double getDurationInColumnHours() {
        LocalDateTime endTime = findNextEntryTime();
        if (endTime != null) {
            return java.time.Duration.between(enteredAt, endTime).toMinutes() / 60.0;
        }
        // Ainda está nesta coluna
        return java.time.Duration.between(enteredAt, LocalDateTime.now()).toMinutes() / 60.0;
    }

    /**
     * Encontra a próxima entrada do card em outra coluna.
     *
     * @return Data/hora da próxima entrada ou null
     */
    private LocalDateTime findNextEntryTime() {
        if (card == null) {
            return null;
        }
        return card.getColumnHistory().stream()
                .filter(h -> h.getEnteredAt().isAfter(this.enteredAt))
                .map(ColumnHistory::getEnteredAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public Card getCard() {
        return card;
    }

    public BoardColumn getColumn() {
        return column;
    }

    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnHistory that = (ColumnHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ColumnHistory{id=%d, column='%s', enteredAt=%s}",
                id, column != null ? column.getName() : "null", enteredAt);
    }
}
