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
import java.time.Duration;
import java.util.Objects;

/**
 * Entidade que representa um registro de bloqueio/desbloqueio de um card.
 *
 * Cada registro armazena o motivo, data/hora e se foi um bloqueio ou desbloqueio.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
@Entity
@Table(name = "blockades", indexes = {
    @Index(name = "idx_blockade_card", columnList = "card_id"),
    @Index(name = "idx_blockade_date", columnList = "blocked_at")
})
public class Blockade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    @Column(name = "is_blocking", nullable = false)
    private Boolean isBlocking;

    /**
     * Construtor padrão requerido pelo JPA.
     */
    public Blockade() {
    }

    /**
     * Construtor com card, motivo e tipo de operação.
     *
     * @param card Card associado
     * @param reason Motivo do bloqueio/desbloqueio
     * @param isBlocking true para bloqueio, false para desbloqueio
     */
    public Blockade(Card card, String reason, Boolean isBlocking) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo do bloqueio/desbloqueio não pode ser vazio");
        }
        this.card = card;
        this.reason = reason;
        this.blockedAt = LocalDateTime.now();
        this.isBlocking = isBlocking;
    }

    /**
     * Calcula a duração deste bloqueio em horas.
     * Se for o último bloqueio e o card ainda estiver bloqueado,
     * calcula até o momento atual.
     *
     * @return Duração em horas
     */
    public double getDurationHours() {
        if (isBlocking) {
            LocalDateTime endTime = findNextUnblockTime();
            if (endTime != null) {
                return Duration.between(blockedAt, endTime).toMinutes() / 60.0;
            }
            // Ainda bloqueado, calcula até agora
            return Duration.between(blockedAt, LocalDateTime.now()).toMinutes() / 60.0;
        }
        return 0;
    }

    /**
     * Encontra o próximo desbloqueio após este bloqueio.
     *
     * @return Data/hora do desbloqueio ou null
     */
    private LocalDateTime findNextUnblockTime() {
        if (card == null) {
            return null;
        }
        return card.getBlockades().stream()
                .filter(b -> !b.getIsBlocking() && b.getBlockedAt().isAfter(this.blockedAt))
                .map(Blockade::getBlockedAt)
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public Boolean getIsBlocking() {
        return isBlocking;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blockade blockade = (Blockade) o;
        return Objects.equals(id, blockade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String operation = isBlocking ? "Bloqueio" : "Desbloqueio";
        return String.format("Blockade{id=%d, %s, reason='%s', at=%s}",
                id, operation, reason, blockedAt);
    }
}
