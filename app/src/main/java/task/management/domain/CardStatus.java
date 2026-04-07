package task.management.domain;

/**
 * Enum que representa o status de um card em relação a bloqueio.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public enum CardStatus {

    ACTIVE("Ativo"),
    BLOCKED("Bloqueado");

    private final String label;

    CardStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
