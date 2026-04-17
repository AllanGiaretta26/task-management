package task.management.service;

import org.junit.jupiter.api.Test;
import task.management.domain.Board;
import task.management.domain.Card;
import task.management.support.AbstractJpaIT;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceIT extends AbstractJpaIT {

    private ReportService newReportService() {
        return new ReportService(emf);
    }

    @Test
    void generateCompletionTimeReport_emptyBoard_showsNoCardsMessage() {
        BoardService boardService = new BoardService(emf);
        Board board = boardService.createBoard("Board vazio", "To Do", "Done", "Cancelled");

        String report = newReportService().generateCompletionTimeReport(board.getId());

        assertTrue(report.contains("RELATÓRIO DE TEMPO DE CONCLUSÃO"));
        assertTrue(report.contains("Nenhum card encontrado"));
    }

    @Test
    void generateCompletionTimeReport_withCompletedCards_computesAverage() {
        BoardService boardService = new BoardService(emf);
        Board board = boardService.createBoard("Fluxo", "To Do", "Done", "Cancelled");
        Long boardId = board.getId();

        CardService cardService = new CardService(emf);
        Card c1 = cardService.createCard(boardId, "Tarefa A", "desc A");
        Card c2 = cardService.createCard(boardId, "Tarefa B", "desc B");
        // Move both to FINAL (next from INITIAL with minimal 3-column board).
        cardService.moveCard(c1.getId(), null);
        cardService.moveCard(c2.getId(), null);

        String report = newReportService().generateCompletionTimeReport(boardId);

        assertTrue(report.contains("Tarefa A"));
        assertTrue(report.contains("Tarefa B"));
        assertTrue(report.contains("Cards concluídos: 2"));
        assertTrue(report.contains("Média de tempo até conclusão"));
    }

    @Test
    void generateBlockadeReport_withBlockUnblockCycle_countsDurations() {
        BoardService boardService = new BoardService(emf);
        Board board = boardService.createBoard("Board B", "To Do", "Done", "Cancelled");
        Long boardId = board.getId();

        CardService cardService = new CardService(emf);
        Card card = cardService.createCard(boardId, "Com bloqueio", "desc");
        cardService.blockCard(card.getId(), "Aguardando dependência externa");
        cardService.unblockCard(card.getId(), "Dependência resolvida");

        String report = newReportService().generateBlockadeReport(boardId);

        assertTrue(report.contains("RELATÓRIO DE BLOQUEIOS"));
        assertTrue(report.contains("Com bloqueio"));
        assertTrue(report.contains("BLOQUEIO"));
        assertTrue(report.contains("DESBLOQUEIO"));
        assertTrue(report.contains("Aguardando dependência externa"));
        assertTrue(report.contains("Cards com bloqueios: 1"));
        assertTrue(report.contains("Total de bloqueios/desbloqueios: 2"));
    }

    @Test
    void generateCompletionTimeReport_unknownBoardId_throws() {
        ReportServiceException ex = assertThrows(ReportServiceException.class, () ->
                newReportService().generateCompletionTimeReport(999_999L));
        assertFalse(ex.getMessage().isBlank());
    }
}
