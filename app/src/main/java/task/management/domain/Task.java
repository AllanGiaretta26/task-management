package task.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade que representa uma tarefa no sistema de gerenciamento.
 *
 * Cada task possui um título, descrição opcional, status de conclusão,
 * prioridade e datas de criação e atualização.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_task_completed", columnList = "completed"),
    @Index(name = "idx_task_priority", columnList = "priority"),
    @Index(name = "idx_task_created_at", columnList = "created_at")
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean completed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Construtor padrão requerido pelo JPA.
     */
    public Task() {
    }

    /**
     * Construtor com título e prioridade.
     *
     * @param title Título da tarefa
     * @param priority Prioridade da tarefa
     */
    public Task(String title, Priority priority) {
        this.title = title;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Construtor com título completo.
     *
     * @param title Título da tarefa
     * @param description Descrição opcional da tarefa
     * @param priority Prioridade da tarefa
     */
    public Task(String title, String description, Priority priority) {
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca a tarefa como concluída.
     */
    public void complete() {
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reabre uma tarefa concluída.
     */
    public void reopen() {
        this.completed = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atualiza o título da tarefa.
     *
     * @param title Novo título
     */
    public void updateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("O título não pode ser vazio");
        }
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atualiza a descrição da tarefa.
     *
     * @param description Nova descrição
     */
    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atualiza a prioridade da tarefa.
     *
     * @param priority Nova prioridade
     */
    public void updatePriority(Priority priority) {
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Priority getPriority() {
        return priority;
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
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Task{id=%d, title='%s', completed=%s, priority=%s}",
                id, title, completed, priority);
    }

    /**
     * Enum que representa os níveis de prioridade de uma tarefa.
     */
    public enum Priority {
        LOW("Baixa"),
        MEDIUM("Média"),
        HIGH("Alta"),
        URGENT("Urgente");

        private final String label;

        Priority(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
