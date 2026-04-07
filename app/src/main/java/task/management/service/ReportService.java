package task.management.service;

import jakarta.persistence.EntityManager;
import task.management.domain.Blockade;
import task.management.domain.Board;
import task.management.domain.Card;
import task.management.domain.ColumnHistory;
import task.management.infrastructure.repository.BoardRepository;
import task.management.infrastructure.repository.CardRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Serviço responsável por gerar relatórios do board.
 *
 * Gera relatórios de tempo de conclusão e bloqueios de cards.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;

    /**
     * Construtor do serviço de relatórios.
     *
     * @param entityManager EntityManager para persistência
     */
    public ReportService(EntityManager entityManager) {
        this.boardRepository = new BoardRepository(entityManager);
        this.cardRepository = new CardRepository(entityManager);
    }

    /**
     * Gera relatório de tempo de conclusão dos cards de um board.
     *
     * Inclui informações do tempo que cada card levou em cada coluna.
     *
     * @param boardId ID do board
     * @return Relatório formatado como string
     */
    public String generateCompletionTimeReport(Long boardId) {
        Board board = findBoardOrThrow(boardId);
        List<Card> cards = cardRepository.findByBoardId(boardId);

        StringBuilder report = new StringBuilder();
        report.append("=" .repeat(80)).append("\n");
        report.append("RELATÓRIO DE TEMPO DE CONCLUSÃO - ").append(board.getName().toUpperCase(BRAZIL)).append("\n");
        report.append("=" .repeat(80)).append("\n\n");

        if (cards.isEmpty()) {
            report.append("Nenhum card encontrado no board.\n");
            return report.toString();
        }

        for (Card card : cards) {
            report.append("Card: ").append(card.getTitle()).append("\n");
            report.append("ID: ").append(card.getId()).append("\n");
            report.append("Status: ").append(card.isBlocked() ? "BLOQUEADO" : "ATIVO").append("\n");
            report.append("Criado em: ").append(card.getCreatedAt().format(DATE_FORMATTER)).append("\n");
            report.append("-".repeat(60)).append("\n");

            // Tempo total até conclusão
            double completionHours = card.getCompletionTimeHours();
            report.append(String.format("Tempo total até coluna final: %.2f horas\n", completionHours));

            // Tempo em cada coluna
            report.append("\nTempo em cada coluna:\n");
            List<ColumnHistory> history = card.getColumnHistory();
            for (int i = 0; i < history.size(); i++) {
                ColumnHistory entry = history.get(i);
                double hours = entry.getDurationInColumnHours();
                String columnName = entry.getColumn().getName();
                String columnType = entry.getColumn().getType().getLabel();
                report.append(String.format("  %d. %s (%s) - %.2f horas (entrou em %s)\n",
                        i + 1, columnName, columnType, hours,
                        entry.getEnteredAt().format(DATE_FORMATTER)));
            }
            report.append("\n");
        }

        // Estatísticas gerais
        report.append("=" .repeat(80)).append("\n");
        report.append("ESTATÍSTICAS GERAIS\n");
        report.append("=" .repeat(80)).append("\n");

        double avgTime = cards.stream()
                .mapToDouble(Card::getCompletionTimeHours)
                .average()
                .orElse(0);

        report.append(String.format("Total de cards: %d\n", cards.size()));
        report.append(String.format("Média de tempo até conclusão: %.2f horas\n", avgTime));

        return report.toString();
    }

    /**
     * Gera relatório de bloqueios dos cards de um board.
     *
     * Inclui tempo que cada card ficou bloqueado e justificativas.
     *
     * @param boardId ID do board
     * @return Relatório formatado como string
     */
    public String generateBlockadeReport(Long boardId) {
        Board board = findBoardOrThrow(boardId);
        List<Card> cards = cardRepository.findByBoardId(boardId);

        StringBuilder report = new StringBuilder();
        report.append("=" .repeat(80)).append("\n");
        report.append("RELATÓRIO DE BLOQUEIOS - ").append(board.getName().toUpperCase(BRAZIL)).append("\n");
        report.append("=" .repeat(80)).append("\n\n");

        if (cards.isEmpty()) {
            report.append("Nenhum card encontrado no board.\n");
            return report.toString();
        }

        int totalBlockades = 0;
        double totalBlockedHours = 0;

        for (Card card : cards) {
            List<Blockade> blockades = card.getBlockades();
            if (blockades.isEmpty()) {
                continue;
            }

            totalBlockades += blockades.size();

            report.append("Card: ").append(card.getTitle()).append("\n");
            report.append("ID: ").append(card.getId()).append("\n");
            report.append("Status atual: ").append(card.isBlocked() ? "BLOQUEADO" : "ATIVO").append("\n");
            report.append("-".repeat(60)).append("\n");

            double cardBlockedHours = card.getTotalBlockedHours();
            totalBlockedHours += cardBlockedHours;

            report.append(String.format("Tempo total bloqueado: %.2f horas\n\n", cardBlockedHours));

            report.append("Histórico de bloqueios/desbloqueios:\n");
            for (Blockade blockade : blockades) {
                String operation = blockade.getIsBlocking() ? "BLOQUEIO" : "DESBLOQUEIO";
                report.append(String.format("  [%s] %s\n", operation,
                        blockade.getBlockedAt().format(DATE_FORMATTER)));
                report.append(String.format("  Motivo: %s\n", blockade.getReason()));

                if (blockade.getIsBlocking()) {
                    double duration = blockade.getDurationHours();
                    report.append(String.format("  Duração do bloqueio: %.2f horas\n", duration));
                }
                report.append("\n");
            }
            report.append("\n");
        }

        // Estatísticas gerais
        report.append("=" .repeat(80)).append("\n");
        report.append("ESTATÍSTICAS GERAIS\n");
        report.append("=" .repeat(80)).append("\n");

        long cardsWithBlockades = cards.stream()
                .filter(c -> !c.getBlockades().isEmpty())
                .count();

        report.append(String.format("Total de cards: %d\n", cards.size()));
        report.append(String.format("Cards com bloqueios: %d\n", cardsWithBlockades));
        report.append(String.format("Total de bloqueios/desbloqueios: %d\n", totalBlockades));
        report.append(String.format("Tempo total de bloqueio: %.2f horas\n", totalBlockedHours));

        if (cardsWithBlockades > 0) {
            double avgBlockedTime = totalBlockedHours / cardsWithBlockades;
            report.append(String.format("Média de tempo bloqueado por card: %.2f horas\n", avgBlockedTime));
        }

        return report.toString();
    }

    /**
     * Busca um board ou lança exceção se não encontrado.
     *
     * @param boardId ID do board
     * @return Board encontrado
     */
    private Board findBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ReportServiceException("Board não encontrado com ID: " + boardId));
    }
}
