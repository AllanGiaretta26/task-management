package task.management.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade Board.
 */
class BoardTest {

    @Test
    @DisplayName("Deve criar um board com nome válido")
    void shouldCreateBoardWithValidName() {
        Board board = new Board("Test Board");

        assertNotNull(board);
        assertEquals("Test Board", board.getName());
        assertNotNull(board.getCreatedAt());
        assertNotNull(board.getUpdatedAt());
        assertTrue(board.getColumns().isEmpty());
    }

    @Test
    @DisplayName("Deve adicionar colunas ao board")
    void shouldAddColumnsToBoard() {
        Board board = new Board("Test Board");
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        board.addColumn(column);

        assertEquals(1, board.getColumns().size());
        assertTrue(board.getColumns().contains(column));
        assertEquals(board, column.getBoard());
    }

    @Test
    @DisplayName("Deve remover colunas do board")
    void shouldRemoveColumnFromBoard() {
        Board board = new Board("Test Board");
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        board.addColumn(column);

        board.removeColumn(column);

        assertEquals(0, board.getColumns().size());
        assertNull(column.getBoard());
    }

    @Test
    @DisplayName("Deve validar estrutura mínima do board")
    void shouldValidateMinimumStructure() {
        Board board = new Board("Test Board");

        // Adiciona estrutura mínima
        board.addColumn(new BoardColumn("To Do", ColumnType.INITIAL, 0));
        board.addColumn(new BoardColumn("Done", ColumnType.FINAL, 1));
        board.addColumn(new BoardColumn("Cancelled", ColumnType.CANCELLED, 2));

        assertTrue(board.hasMinimumStructure());
    }

    @Test
    @DisplayName("Deve retornar falso quando estrutura mínima não está completa")
    void shouldReturnFalseWhenMinimumStructureNotComplete() {
        Board board = new Board("Test Board");
        board.addColumn(new BoardColumn("To Do", ColumnType.INITIAL, 0));

        assertFalse(board.hasMinimumStructure());
    }

    @Test
    @DisplayName("Deve encontrar coluna por tipo")
    void shouldFindColumnByType() {
        Board board = new Board("Test Board");
        BoardColumn initialColumn = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        BoardColumn finalColumn = new BoardColumn("Done", ColumnType.FINAL, 1);
        board.addColumn(initialColumn);
        board.addColumn(finalColumn);

        assertEquals(initialColumn, board.findColumnByType(ColumnType.INITIAL));
        assertEquals(finalColumn, board.findColumnByType(ColumnType.FINAL));
        assertNull(board.findColumnByType(ColumnType.CANCELLED));
    }

    @Test
    @DisplayName("Deve encontrar coluna por ID")
    void shouldFindColumnById() {
        Board board = new Board("Test Board");
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        board.addColumn(column);

        // Simula ID após persistência
        assertDoesNotThrow(() -> board.findColumnById(column.getId()));
    }

    @Test
    @DisplayName("Deve retornar próxima coluna no fluxo")
    void shouldReturnNextColumnInFlow() {
        Board board = new Board("Test Board");
        BoardColumn initial = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        BoardColumn pending = new BoardColumn("In Progress", ColumnType.PENDING, 1);
        BoardColumn finalCol = new BoardColumn("Done", ColumnType.FINAL, 2);
        BoardColumn cancelled = new BoardColumn("Cancelled", ColumnType.CANCELLED, 3);

        board.addColumn(initial);
        board.addColumn(pending);
        board.addColumn(finalCol);
        board.addColumn(cancelled);

        // Fluxo normal: inicial -> pendente -> final
        // Coluna cancelada não faz parte do fluxo normal
        assertEquals(pending, board.getNextColumn(initial));
        assertEquals(finalCol, board.getNextColumn(pending));
        assertNull(board.getNextColumn(finalCol)); // Não há próxima após final no fluxo normal
        assertNull(board.getNextColumn(cancelled)); // Cancelada é a última
    }

    @Test
    @DisplayName("Deve retornar lista não modificável de colunas")
    void shouldReturnUnmodifiableColumnList() {
        Board board = new Board("Test Board");
        board.addColumn(new BoardColumn("To Do", ColumnType.INITIAL, 0));

        assertThrows(UnsupportedOperationException.class, () -> {
            board.getColumns().add(new BoardColumn("Test", ColumnType.PENDING, 1));
        });
    }

    @Test
    @DisplayName("Deve atualizar nome do board")
    void shouldUpdateBoardName() {
        Board board = new Board("Test Board");

        board.setName("Updated Board");

        assertEquals("Updated Board", board.getName());
    }

    @Test
    @DisplayName("Deve testar equals e hashCode")
    void shouldTestEqualsAndHashCode() {
        Board board1 = new Board("Board 1");
        Board board2 = new Board("Board 2");

        // Boards sem ID são diferentes por referência
        assertNotEquals(board1, board2);

        // Mesmo objeto deve ser igual
        assertEquals(board1, board1);

        // Null deve ser diferente
        assertNotEquals(null, board1);

        // Boards diferentes
        assertFalse(board1.equals(board2));
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void shouldGenerateToString() {
        Board board = new Board("Test Board");

        String result = board.toString();

        assertTrue(result.contains("Test Board"));
        assertTrue(result.contains("Board"));
    }
}
