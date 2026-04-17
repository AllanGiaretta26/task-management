package task.management.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade Card.
 */
class CardTest {

    private Card card;

    @BeforeEach
    void setUp() {
        card = new Card("Test Card", "Test Description");
    }

    @Test
    @DisplayName("Deve criar card com título e descrição válidos")
    void shouldCreateCardWithValidTitleAndDescription() {
        assertNotNull(card);
        assertEquals("Test Card", card.getTitle());
        assertEquals("Test Description", card.getDescription());
        assertNotNull(card.getCreatedAt());
        assertEquals(CardStatus.ACTIVE, card.getStatus());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar card com título vazio")
    void shouldThrowExceptionWhenCreatingCardWithEmptyTitle() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Card("", "Description");
        });
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar card com título nulo")
    void shouldThrowExceptionWhenCreatingCardWithNullTitle() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Card(null, "Description");
        });
    }

    @Test
    @DisplayName("Deve bloquear card com motivo")
    void shouldBlockCardWithReason() {
        card.block("Aguardando aprovação");

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        assertTrue(card.isBlocked());
        assertEquals(1, card.getBlockades().size());

        Blockade blockade = card.getBlockades().get(0);
        assertTrue(blockade.getIsBlocking());
        assertEquals("Aguardando aprovação", blockade.getReason());
    }

    @Test
    @DisplayName("Deve lançar exceção ao bloquear card já bloqueado")
    void shouldThrowExceptionWhenBlockingAlreadyBlockedCard() {
        card.block("Reason 1");

        assertThrows(IllegalStateException.class, () -> {
            card.block("Reason 2");
        });
    }

    @Test
    @DisplayName("Deve desbloquear card com motivo")
    void shouldUnblockCardWithReason() {
        card.block("Reason");
        card.unblock("Aprovado");

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        assertFalse(card.isBlocked());
        assertEquals(2, card.getBlockades().size());

        Blockade unblockBlockade = card.getBlockades().get(1);
        assertFalse(unblockBlockade.getIsBlocking());
        assertEquals("Aprovado", unblockBlockade.getReason());
    }

    @Test
    @DisplayName("Deve lançar exceção ao desbloquear card não bloqueado")
    void shouldThrowExceptionWhenUnblockingNonBlockedCard() {
        assertThrows(IllegalStateException.class, () -> {
            card.unblock("Reason");
        });
    }

    @Test
    @DisplayName("Deve registrar entrada em coluna")
    void shouldRecordColumnEntry() {
        BoardColumn column = new BoardColumn("To Do", ColumnType.INITIAL, 0);

        card.recordColumnEntry(column);

        assertEquals(1, card.getColumnHistory().size());
        assertEquals(column, card.getColumnHistory().get(0).getColumn());
        assertNotNull(card.getColumnHistory().get(0).getEnteredAt());
    }

    @Test
    @DisplayName("Deve calcular tempo total de bloqueio")
    void shouldCalculateTotalBlockedHours() {
        card.block("Reason");
        card.unblock("Unblocked");

        double totalHours = card.getTotalBlockedHours();
        assertTrue(totalHours >= 0);
    }

    @Test
    @DisplayName("Deve calcular tempo de conclusão quando card passa pela coluna final")
    void shouldCalculateCompletionTimeHours() {
        BoardColumn col1 = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        BoardColumn col2 = new BoardColumn("Done", ColumnType.FINAL, 1);

        card.recordColumnEntry(col1);
        card.recordColumnEntry(col2);

        java.util.Optional<Double> hours = card.getCompletionTimeHours();
        assertTrue(hours.isPresent(), "Card que entrou na coluna final deve ter tempo de conclusão");
        assertTrue(hours.get() >= 0);
    }

    @Test
    @DisplayName("Deve retornar vazio quando card não passou pela coluna final")
    void shouldReturnEmptyWhenCardNotCompleted() {
        BoardColumn col1 = new BoardColumn("To Do", ColumnType.INITIAL, 0);
        BoardColumn col2 = new BoardColumn("In Progress", ColumnType.PENDING, 1);

        card.recordColumnEntry(col1);
        card.recordColumnEntry(col2);

        assertTrue(card.getCompletionTimeHours().isEmpty(),
                "Card ainda em andamento não deve ter tempo de conclusão");
    }

    @Test
    @DisplayName("Deve retornar lista não modificável de bloqueios")
    void shouldReturnUnmodifiableBlockadesList() {
        assertThrows(UnsupportedOperationException.class, () -> {
            card.getBlockades().add(new Blockade());
        });
    }

    @Test
    @DisplayName("Deve retornar lista não modificável de histórico")
    void shouldReturnUnmodifiableColumnHistoryList() {
        assertThrows(UnsupportedOperationException.class, () -> {
            card.getColumnHistory().add(new ColumnHistory());
        });
    }

    @Test
    @DisplayName("Deve atualizar título do card")
    void shouldUpdateCardTitle() {
        card.setTitle("Updated Title");

        assertEquals("Updated Title", card.getTitle());
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar título para vazio")
    void shouldThrowExceptionWhenUpdatingTitleToEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            card.setTitle("");
        });
    }

    @Test
    @DisplayName("Deve atualizar descrição do card")
    void shouldUpdateCardDescription() {
        card.setDescription("Updated Description");

        assertEquals("Updated Description", card.getDescription());
    }

    @Test
    @DisplayName("Deve testar equals e hashCode")
    void shouldTestEqualsAndHashCode() {
        Card card1 = new Card("Card 1", "Description 1");
        Card card2 = new Card("Card 2", "Description 2");

        // Cards sem ID são diferentes por referência
        assertNotEquals(card1, card2);

        // Mesmo objeto deve ser igual
        assertEquals(card1, card1);

        // Null deve ser diferente
        assertNotEquals(null, card1);

        // Cards diferentes
        assertFalse(card1.equals(card2));
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void shouldGenerateToString() {
        String result = card.toString();

        assertTrue(result.contains("Test Card"));
        assertTrue(result.contains("Card"));
    }
}
