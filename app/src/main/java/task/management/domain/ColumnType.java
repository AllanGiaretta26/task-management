package task.management.domain;

/**
 * Enum que representa os tipos de coluna em um board.
 *
 * Cada coluna em um board deve ter um tipo definido que determina
 * seu comportamento e posição no fluxo de trabalho.
 *
 * INITIAL: Coluna onde os cards são criados (deve ser a primeira)
 * PENDING: Colunas intermediárias para cards em andamento (pode ter várias)
 * FINAL: Coluna para cards concluídos (deve ser a penúltima)
 * CANCELLED: Coluna para cards cancelados (deve ser a última)
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public enum ColumnType {

    INITIAL("Inicial"),
    PENDING("Pendente"),
    FINAL("Final"),
    CANCELLED("Cancelada");

    private final String label;

    ColumnType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
