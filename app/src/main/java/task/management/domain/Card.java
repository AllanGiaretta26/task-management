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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidade que representa um card em uma coluna do board.
 *
 * Um card possui título, descrição, data de criação, status de bloqueio
 * e histórico de movimentação entre colunas.
 *
 * @author Allan Giaretta
 * @version 2.0
 */
@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_column", columnList = "column_id"),
    @Index(name = "idx_card_status", columnList = "status")
})
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private CardStatus status = CardStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("blockedAt ASC")
    private List<Blockade> blockades = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("enteredAt ASC")
    private List<ColumnHistory> columnHistory = new ArrayList<>();

    /**
     * Construtor padrão requerido pelo JPA.
     */
    public Card() {
    }

    /**
     * Construtor com título e descrição.
     *
     * @param title Título do card
     * @param description Descrição do card
     */
    public Card(String title, String description) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("O título do card não pode ser vazio");
        }
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.status = CardStatus.ACTIVE;
    }

    /**
     * Bloqueia o card com uma justificativa.
     *
     * @param reason Motivo do bloqueio
     */
    public void block(String reason) {
        if (this.status == CardStatus.BLOCKED) {
            throw new IllegalStateException("O card já está bloqueado");
        }
        this.status = CardStatus.BLOCKED;
        Blockade blockade = new Blockade(this, reason, true);
        this.blockades.add(blockade);
    }

    /**
     * Desbloqueia o card com uma justificativa.
     *
     * @param reason Motivo do desbloqueio
     */
    public void unblock(String reason) {
        if (this.status != CardStatus.BLOCKED) {
            throw new IllegalStateException("O card não está bloqueado");
        }
        this.status = CardStatus.ACTIVE;
        Blockade blockade = new Blockade(this, reason, false);
        this.blockades.add(blockade);
    }

    /**
     * Registra a entrada do card em uma coluna.
     *
     * @param column Coluna onde o card entrou
     */
    public void recordColumnEntry(BoardColumn column) {
        ColumnHistory history = new ColumnHistory(this, column);
        this.columnHistory.add(history);
    }

    /**
     * Retorna a lista não modificável de bloqueios.
     *
     * @return Lista de bloqueios
     */
    public List<Blockade> getBlockades() {
        return Collections.unmodifiableList(blockades);
    }

    /**
     * Retorna a lista não modificável do histórico de colunas.
     *
     * @return Lista de histórico de colunas
     */
    public List<ColumnHistory> getColumnHistory() {
        return Collections.unmodifiableList(columnHistory);
    }

    /**
     * Verifica se o card está bloqueado.
     *
     * @return true se estiver bloqueado
     */
    public boolean isBlocked() {
        return status == CardStatus.BLOCKED;
    }

    /**
     * Calcula o tempo total que o card ficou bloqueado.
     *
     * @return Tempo total de bloqueio em horas
     */
    public double getTotalBlockedHours() {
        return blockades.stream()
                .mapToDouble(Blockade::getDurationHours)
                .sum();
    }

    /**
     * Calcula o tempo que o card levou para ser concluído.
     *
     * Considerado "concluído" apenas quando o card entrou na coluna FINAL.
     * Cards cancelados ou ainda em andamento retornam {@code Optional.empty()}.
     *
     * @return Tempo em horas da primeira entrada até a entrada na coluna final,
     *         ou {@code Optional.empty()} se o card não foi concluído
     */
    public Optional<Double> getCompletionTimeHours() {
        if (columnHistory.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime firstEntry = columnHistory.stream()
                .map(ColumnHistory::getEnteredAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime finalEntry = columnHistory.stream()
                .filter(h -> h.getColumn() != null && h.getColumn().getType() == ColumnType.FINAL)
                .map(ColumnHistory::getEnteredAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        if (firstEntry == null || finalEntry == null) {
            return Optional.empty();
        }

        double hours = Duration.between(firstEntry, finalEntry).toMinutes() / 60.0;
        return Optional.of(hours);
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("O título do card não pode ser vazio");
        }
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CardStatus getStatus() {
        return status;
    }

    public BoardColumn getColumn() {
        return column;
    }

    public void setColumn(BoardColumn column) {
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        if (id == null || card.id == null) {
            return false;
        }
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Card{id=%d, title='%s', status=%s}",
                id, title, status);
    }
}
