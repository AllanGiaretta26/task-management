package task.management.service;

import org.junit.jupiter.api.Test;
import task.management.domain.Board;
import task.management.domain.Card;
import task.management.domain.ColumnType;
import task.management.support.AbstractJpaIT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardServiceIT extends AbstractJpaIT {

    private static class BoardSetup {
        final Long boardId;
        final Long initialColumnId;
        final Long pendingColumnId;
        final Long finalColumnId;
        final Long cancelledColumnId;

        BoardSetup(Long boardId, Long initialColumnId, Long pendingColumnId,
                   Long finalColumnId, Long cancelledColumnId) {
            this.boardId = boardId;
            this.initialColumnId = initialColumnId;
            this.pendingColumnId = pendingColumnId;
            this.finalColumnId = finalColumnId;
            this.cancelledColumnId = cancelledColumnId;
        }
    }

    private BoardSetup setupBoardWithPending() {
        BoardService boardService = new BoardService(emf);
        Board board = boardService.createBoard("Fluxo", "To Do", "Done", "Cancelled");
        boardService.addPendingColumn(board.getId(), "In Progress");
        Board reloaded = new BoardService(emf).findBoardById(board.getId());
        return new BoardSetup(
                reloaded.getId(),
                reloaded.findColumnByType(ColumnType.INITIAL).getId(),
                reloaded.findColumnByType(ColumnType.PENDING).getId(),
                reloaded.findColumnByType(ColumnType.FINAL).getId(),
                reloaded.findColumnByType(ColumnType.CANCELLED).getId());
    }

    private BoardSetup setupBoardMinimal() {
        BoardService boardService = new BoardService(emf);
        Board board = boardService.createBoard("Mínimo", "To Do", "Done", "Cancelled");
        Board reloaded = new BoardService(emf).findBoardById(board.getId());
        return new BoardSetup(
                reloaded.getId(),
                reloaded.findColumnByType(ColumnType.INITIAL).getId(),
                null,
                reloaded.findColumnByType(ColumnType.FINAL).getId(),
                reloaded.findColumnByType(ColumnType.CANCELLED).getId());
    }

    private CardService newCardService() {
        return new CardService(emf);
    }

    @Test
    void createCard_putsCardOnInitialColumnAndRecordsHistory() {
        BoardSetup setup = setupBoardMinimal();
        CardService service = newCardService();

        Card card = service.createCard(setup.boardId, "Implementar login", "Tela + validação");

        assertNotNull(card.getId());
        assertEquals(setup.initialColumnId, card.getColumn().getId());
        assertEquals(ColumnType.INITIAL, card.getColumn().getType());
        assertEquals(1, card.getColumnHistory().size());
        assertEquals(setup.initialColumnId,
                card.getColumnHistory().get(0).getColumn().getId());
    }

    @Test
    void moveCard_toNextColumnSequential_updatesColumnAndHistory() {
        BoardSetup setup = setupBoardWithPending();
        CardService service = newCardService();
        Card card = service.createCard(setup.boardId, "Task", "desc");

        Card moved = service.moveCard(card.getId(), null);

        assertEquals(setup.pendingColumnId, moved.getColumn().getId());
        assertEquals(2, moved.getColumnHistory().size());
    }

    @Test
    void moveCard_skippingColumn_throws() {
        BoardSetup setup = setupBoardWithPending();
        CardService service = newCardService();
        Card card = service.createCard(setup.boardId, "Task", "desc");

        CardServiceException ex = assertThrows(CardServiceException.class, () ->
                service.moveCard(card.getId(), setup.finalColumnId));
        assertTrue(ex.getMessage().toLowerCase().contains("pular"));
    }

    @Test
    void cancelCard_fromIntermediateColumn_movesToCancelled() {
        BoardSetup setup = setupBoardWithPending();
        CardService service = newCardService();
        Card card = service.createCard(setup.boardId, "Task", "desc");
        service.moveCard(card.getId(), null);

        Card cancelled = service.cancelCard(card.getId());

        assertEquals(setup.cancelledColumnId, cancelled.getColumn().getId());
        assertEquals(ColumnType.CANCELLED, cancelled.getColumn().getType());
    }

    @Test
    void blockCard_thenMove_throwsBecauseBlocked() {
        BoardSetup setup = setupBoardWithPending();
        CardService service = newCardService();
        Card card = service.createCard(setup.boardId, "Task", "desc");

        service.blockCard(card.getId(), "Aguardando revisão de design");

        CardServiceException ex = assertThrows(CardServiceException.class, () ->
                service.moveCard(card.getId(), null));
        assertTrue(ex.getMessage().toLowerCase().contains("bloqueado"));
    }

    @Test
    void unblockCard_allowsSubsequentMove() {
        BoardSetup setup = setupBoardWithPending();
        CardService service = newCardService();
        Card card = service.createCard(setup.boardId, "Task", "desc");
        service.blockCard(card.getId(), "Aguardando");
        service.unblockCard(card.getId(), "Resolvido");

        Card moved = service.moveCard(card.getId(), null);

        assertEquals(setup.pendingColumnId, moved.getColumn().getId());
    }
}
