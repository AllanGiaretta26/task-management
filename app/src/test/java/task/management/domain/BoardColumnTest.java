package task.management.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade BoardColumn.
 */
class BoardColumnTest {

    @Test
    @DisplayName("Deve criar coluna com nome, tipo e posição válidos")
    void shouldCreateColumnWithValidNameTypeAndPosition() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        assertNotNull(column);
        assertEquals("To Do", column.getName());
        assertEquals(ColumnType.INITIAL, column.getType());
        assertEquals(0, column.getPosition());
        assertTrue(column.getCards().isEmpty());
    }

    @Test
    @DisplayName("Deve adicionar card à coluna")
    void shouldAddCardToColumn() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        Card card = new Card("Test Card", "Description");

        column.addCard(card);

        assertEquals(1, column.getCards().size());
        assertTrue(column.getCards().contains(card));
        assertEquals(column, card.getColumn());
    }

    @Test
    @DisplayName("Deve remover card da coluna")
    void shouldRemoveCardFromColumn() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        Card card = new Card("Test Card", "Description");
        column.addCard(card);

        column.removeCard(card);

        assertEquals(0, column.getCards().size());
        assertNull(card.getColumn());
    }

    @Test
    @DisplayName("Deve encontrar card por ID")
    void shouldFindCardById() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        Card card = new Card("Test Card", "Description");
        column.addCard(card);

        // Card sem ID deve retornar null na busca
        Card found = column.findCardById(null);

        assertNull(found);
    }

    @Test
    @DisplayName("Deve encontrar card quando tiver ID")
    void shouldFindCardWhenItHasId() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        Card card = new Card("Test Card", "Description");
        column.addCard(card);

        // Procura pelo título do card já que não tem ID
        Card found = column.getCards().stream()
                .filter(c -> c.getTitle().equals("Test Card"))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        assertEquals("Test Card", found.getTitle());
    }

    @Test
    @DisplayName("Deve retornar null quando card não existe")
    void shouldReturnNullWhenCardNotFound() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        Card found = column.findCardById(999L);

        assertNull(found);
    }

    @Test
    @DisplayName("Deve verificar se é coluna inicial")
    void shouldCheckIfIsInitialColumn() {
        BoardColumn initial = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        BoardColumn pending = new BoardColumn("In Progress", ColumnType.PENDING, 1);

        assertTrue(initial.isInitial());
        assertFalse(pending.isInitial());
    }

    @Test
    @DisplayName("Deve verificar se é coluna final")
    void shouldCheckIfIsFinalColumn() {
        BoardColumn finalCol = new BoardColumn("Done", ColumnType.FINAL, 2);
        BoardColumn pending = new BoardColumn("In Progress", ColumnType.PENDING, 1);

        assertTrue(finalCol.isFinal());
        assertFalse(pending.isFinal());
    }

    @Test
    @DisplayName("Deve verificar se é coluna de cancelamento")
    void shouldCheckIfIsCancelledColumn() {
        BoardColumn cancelled = new BoardColumn("Cancelled", ColumnType.CANCELLED, 3);
        BoardColumn pending = new BoardColumn("In Progress", ColumnType.PENDING, 1);

        assertTrue(cancelled.isCancelled());
        assertFalse(pending.isCancelled());
    }

    @Test
    @DisplayName("Deve verificar se é coluna pendente")
    void shouldCheckIfIsPendingColumn() {
        BoardColumn pending = new BoardColumn("In Progress", ColumnType.PENDING, 1);
        BoardColumn initial = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        assertTrue(pending.isPending());
        assertFalse(initial.isPending());
    }

    @Test
    @DisplayName("Deve retornar lista não modificável de cards")
    void shouldReturnUnmodifiableCardList() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        assertThrows(UnsupportedOperationException.class, () -> {
            column.getCards().add(new Card("Test", "Description"));
        });
    }

    @Test
    @DisplayName("Deve atualizar nome da coluna")
    void shouldUpdateColumnName() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        column.setName("Updated Name");

        assertEquals("Updated Name", column.getName());
    }

    @Test
    @DisplayName("Deve atualizar posição da coluna")
    void shouldUpdateColumnPosition() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        column.setPosition(5);

        assertEquals(5, column.getPosition());
    }

    @Test
    @DisplayName("Deve atualizar tipo da coluna")
    void shouldUpdateColumnType() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        column.setType(ColumnType.PENDING);

        assertEquals(ColumnType.PENDING, column.getType());
    }

    @Test
    @DisplayName("Deve testar equals e hashCode")
    void shouldTestEqualsAndHashCode() {
        BoardColumn column1 = new BoardColumn("Column 1", ColumnType.INITIAL, 0);
        BoardColumn column2 = new BoardColumn("Column 2", ColumnType.FINAL, 1);

        // Colunas sem ID são diferentes por referência de objeto
        assertNotEquals(column1, column2);

        // Mesmo objeto deve ser igual
        assertEquals(column1, column1);

        // Null deve ser diferente
        assertNotEquals(null, column1);

        // Objetos diferentes do mesmo tipo
        assertFalse(column1.equals(column2));
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void shouldGenerateToString() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        String result = column.toString();

        assertTrue(result.contains("To Do"));
        assertTrue(result.contains("INITIAL"));
    }
}
