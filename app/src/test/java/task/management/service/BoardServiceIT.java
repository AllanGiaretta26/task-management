package task.management.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import task.management.domain.Board;
import task.management.domain.BoardColumn;
import task.management.domain.ColumnType;
import task.management.support.AbstractJpaIT;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardServiceIT extends AbstractJpaIT {

    private BoardService newService() {
        return new BoardService(emf);
    }

    @Test
    void createBoard_persistsWith3MandatoryColumns() {
        BoardService service = newService();

        Board board = service.createBoard("Sprint 1", "To Do", "Done", "Cancelled");

        assertNotNull(board.getId());
        List<BoardColumn> columns = board.getColumns();
        assertEquals(3, columns.size());
        assertEquals(ColumnType.INITIAL, columns.get(0).getType());
        assertEquals(ColumnType.FINAL, columns.get(1).getType());
        assertEquals(ColumnType.CANCELLED, columns.get(2).getType());
    }

    @Test
    void createBoard_rejectsEmptyName() {
        BoardService service = newService();

        BoardServiceException ex = assertThrows(BoardServiceException.class, () ->
                service.createBoard("  ", "To Do", "Done", "Cancelled"));
        assertTrue(ex.getMessage().toLowerCase().contains("vazio"));
    }

    @Test
    void addPendingColumn_insertsBeforeFinalAndShiftsPositions() {
        BoardService service = newService();
        Board created = service.createBoard("Board A", "Start", "End", "Trash");

        service.addPendingColumn(created.getId(), "In Progress");

        // Re-fetch num EM fresco para garantir ordenação por @OrderBy("position ASC").
        Board reloaded = new BoardService(emf).findBoardById(created.getId());
        List<BoardColumn> columns = reloaded.getColumns();
        assertEquals(4, columns.size());
        assertEquals(ColumnType.INITIAL, columns.get(0).getType());
        assertEquals(ColumnType.PENDING, columns.get(1).getType());
        assertEquals("In Progress", columns.get(1).getName());
        // FINAL e CANCELLED vêm em seguida; ordem relativa entre elas é indiferente aqui.
        assertTrue(columns.subList(2, 4).stream().anyMatch(c -> c.getType() == ColumnType.FINAL));
        assertTrue(columns.subList(2, 4).stream().anyMatch(c -> c.getType() == ColumnType.CANCELLED));
    }

    @Test
    void findBoardById_returnsBoardWithColumns() {
        BoardService service = newService();
        Board saved = service.createBoard("Board X", "To Do", "Done", "Cancelled");

        Board fetched = service.findBoardById(saved.getId());

        assertEquals(saved.getId(), fetched.getId());
        assertEquals(3, fetched.getColumns().size());
    }

    @Test
    void deleteBoard_cascadesToColumns() {
        BoardService service = newService();
        Board saved = service.createBoard("Board Y", "To Do", "Done", "Cancelled");
        Long boardId = saved.getId();

        service.deleteBoard(boardId);

        BoardServiceException ex = assertThrows(BoardServiceException.class, () ->
                service.findBoardById(boardId));
        assertTrue(ex.getMessage().toLowerCase().contains("não encontrado"));

        // Verifica via query que as colunas foram removidas por cascade
        try (EntityManager em = newEntityManager()) {
            Long remaining = em.createQuery(
                    "SELECT COUNT(c) FROM BoardColumn c WHERE c.board.id = :id", Long.class)
                    .setParameter("id", boardId)
                    .getSingleResult();
            assertEquals(0L, remaining);
        }
    }

    @Test
    void findBoardById_throwsWhenNotFound() {
        BoardService service = newService();

        BoardServiceException ex = assertThrows(BoardServiceException.class, () ->
                service.findBoardById(999_999L));
        assertFalse(ex.getMessage().isBlank());
    }
}
