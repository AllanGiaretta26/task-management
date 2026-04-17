package task.management.ui;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.management.domain.Board;
import task.management.domain.BoardColumn;
import task.management.domain.Card;
import task.management.infrastructure.jpa.JpaUtil;
import task.management.service.BoardService;
import task.management.service.BoardServiceException;
import task.management.service.CardService;
import task.management.service.CardServiceException;
import task.management.service.ReportService;
import task.management.service.ReportServiceException;

import java.util.List;
import java.util.Scanner;

/**
 * Menu interativo para manipulação de boards.
 *
 * Implementa a interface de linha de comando (CLI) para o sistema de gerenciamento de tarefas.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class BoardMenu {

    private static final Logger logger = LoggerFactory.getLogger(BoardMenu.class);

    private final Scanner scanner;
    private final BoardService boardService;
    private final CardService cardService;
    private final ReportService reportService;

    /**
     * Construtor do menu de boards.
     */
    public BoardMenu() {
        this.scanner = new Scanner(System.in);
        EntityManagerFactory emf = JpaUtil.getEntityManagerFactory();
        this.boardService = new BoardService(emf);
        this.cardService = new CardService(emf);
        this.reportService = new ReportService(emf);
    }

    /**
     * Exibe o menu principal e processa as opções escolhidas.
     */
    public void showMainMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("       SISTEMA DE GERENCIAMENTO DE TASKS - BOARD");
            System.out.println("=".repeat(60));
            System.out.println("1. Criar novo board");
            System.out.println("2. Selecionar board");
            System.out.println("3. Excluir board");
            System.out.println("4. Sair");
            System.out.print("\nEscolha uma opção: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> createNewBoard();
                    case "2" -> selectBoard();
                    case "3" -> deleteBoard();
                    case "4" -> {
                        System.out.println("Encerrando o sistema...");
                        running = false;
                    }
                    default -> System.out.println("Opção inválida! Tente novamente.");
                }
            } catch (BoardServiceException | CardServiceException | ReportServiceException e) {
                // Erro de regra de negócio — apenas informa o usuário
                System.out.println("Erro: " + e.getMessage());
            } catch (RuntimeException e) {
                // Bug inesperado — registra com stack trace para diagnóstico
                logger.error("Erro inesperado no menu principal", e);
                System.out.println("Erro inesperado: " + e.getMessage()
                        + " (consulte o log para mais detalhes)");
            }
        }
    }

    /**
     * Cria um novo board com a estrutura mínima de colunas.
     */
    private void createNewBoard() {
        System.out.println("\n--- CRIAR NOVO BOARD ---");
        System.out.print("Nome do board: ");
        String boardName = scanner.nextLine().trim();

        System.out.print("Nome da coluna inicial (ex: 'A Fazer'): ");
        String initialColumn = scanner.nextLine().trim();

        System.out.print("Nome da coluna final (ex: 'Concluído'): ");
        String finalColumn = scanner.nextLine().trim();

        System.out.print("Nome da coluna de cancelamento (ex: 'Cancelado'): ");
        String cancelledColumn = scanner.nextLine().trim();

        Board board = boardService.createBoard(boardName, initialColumn, finalColumn, cancelledColumn);

        System.out.println("\nBoard criado com sucesso!");
        System.out.println("ID: " + board.getId());
        System.out.println("Nome: " + board.getName());
        System.out.println("Colunas criadas:");
        board.getColumns().forEach(col ->
                System.out.println("  - " + col.getName() + " (" + col.getType().getLabel() + ")"));
    }

    /**
     * Lista os boards e permite selecionar um para manipulação.
     */
    private void selectBoard() {
        List<Board> boards = boardService.listAllBoards();

        if (boards.isEmpty()) {
            System.out.println("\nNenhum board encontrado. Crie um novo board primeiro.");
            return;
        }

        System.out.println("\n--- BOARDS DISPONÍVEIS ---");
        for (int i = 0; i < boards.size(); i++) {
            Board board = boards.get(i);
            System.out.printf("%d. %s (ID: %d, Colunas: %d)%n",
                    i + 1, board.getName(), board.getId(), board.getColumns().size());
        }

        System.out.print("\nSelecione o número do board: ");
        String choice = scanner.nextLine().trim();

        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < boards.size()) {
                Board selectedBoard = boards.get(index);
                showBoardMenu(selectedBoard);
            } else {
                System.out.println("Seleção inválida!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    /**
     * Exclui um board existente.
     */
    private void deleteBoard() {
        List<Board> boards = boardService.listAllBoards();

        if (boards.isEmpty()) {
            System.out.println("\nNenhum board encontrado.");
            return;
        }

        System.out.println("\n--- EXCLUIR BOARD ---");
        for (int i = 0; i < boards.size(); i++) {
            Board board = boards.get(i);
            System.out.printf("%d. %s (ID: %d)%n", i + 1, board.getName(), board.getId());
        }

        System.out.print("\nSelecione o número do board para excluir: ");
        String choice = scanner.nextLine().trim();

        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < boards.size()) {
                Board boardToDelete = boards.get(index);
                System.out.print("Tem certeza que deseja excluir o board '" + boardToDelete.getName() + "'? (s/n): ");
                String confirm = scanner.nextLine().trim().toLowerCase();

                if ("s".equals(confirm)) {
                    boardService.deleteBoard(boardToDelete.getId());
                    System.out.println("Board excluído com sucesso!");
                } else {
                    System.out.println("Operação cancelada.");
                }
            } else {
                System.out.println("Seleção inválida!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    /**
     * Exibe o menu de manipulação de um board selecionado.
     *
     * @param board Board selecionado
     */
    private void showBoardMenu(Board board) {
        boolean running = true;

        while (running) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("       BOARD: " + board.getName().toUpperCase());
            System.out.println("=".repeat(60));
            System.out.println("1. Visualizar board e cards");
            System.out.println("2. Criar novo card");
            System.out.println("3. Mover card para próxima coluna");
            System.out.println("4. Cancelar card");
            System.out.println("5. Bloquear card");
            System.out.println("6. Desbloquear card");
            System.out.println("7. Gerar relatório de tempo de conclusão");
            System.out.println("8. Gerar relatório de bloqueios");
            System.out.println("9. Adicionar coluna pendente");
            System.out.println("10. Fechar board");
            System.out.print("\nEscolha uma opção: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> viewBoard(board);
                    case "2" -> createCard(board);
                    case "3" -> moveCard(board);
                    case "4" -> cancelCard(board);
                    case "5" -> blockCard(board);
                    case "6" -> unblockCard(board);
                    case "7" -> generateCompletionReport(board);
                    case "8" -> generateBlockadeReport(board);
                    case "9" -> addPendingColumn(board);
                    case "10" -> {
                        System.out.println("Fechando board...");
                        running = false;
                    }
                    default -> System.out.println("Opção inválida! Tente novamente.");
                }

                // Recarrega o board para manter os dados atualizados
                board = boardService.findBoardById(board.getId());
            } catch (BoardServiceException | CardServiceException | ReportServiceException e) {
                System.out.println("Erro: " + e.getMessage());
            } catch (RuntimeException e) {
                logger.error("Erro inesperado no menu do board '{}'", board.getName(), e);
                System.out.println("Erro inesperado: " + e.getMessage()
                        + " (consulte o log para mais detalhes)");
            }
        }
    }

    /**
     * Visualiza o board com suas colunas e cards.
     *
     * @param board Board a ser visualizado
     */
    private void viewBoard(Board board) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("BOARD: " + board.getName());
        System.out.println("-".repeat(60));

        board.getColumns().forEach(column -> {
            System.out.println("\n[" + column.getName() + "] (" + column.getType().getLabel() + ")");
            System.out.println("  " + "─".repeat(40));

            if (column.getCards().isEmpty()) {
                System.out.println("  (nenhum card)");
            } else {
                column.getCards().forEach(card -> {
                    String status = card.isBlocked() ? " [BLOQUEADO]" : "";
                    System.out.println("  • " + card.getTitle() + status);
                    if (card.getDescription() != null && !card.getDescription().isEmpty()) {
                        System.out.println("    " + card.getDescription());
                    }
                });
            }
        });
    }

    /**
     * Cria um novo card na coluna inicial do board.
     *
     * @param board Board onde o card será criado
     */
    private void createCard(Board board) {
        System.out.println("\n--- CRIAR NOVO CARD ---");
        System.out.print("Título do card: ");
        String title = scanner.nextLine().trim();

        System.out.print("Descrição (opcional): ");
        String description = scanner.nextLine().trim();

        Card card = cardService.createCard(board.getId(), title, description);

        System.out.println("\nCard criado com sucesso!");
        System.out.println("ID: " + card.getId());
        System.out.println("Título: " + card.getTitle());
    }

    /**
     * Move um card para a próxima coluna.
     *
     * @param board Board onde o card está
     */
    private void moveCard(Board board) {
        List<Card> cards = cardService.listCardsByBoard(board.getId());

        if (cards.isEmpty()) {
            System.out.println("\nNenhum card encontrado no board.");
            return;
        }

        System.out.println("\n--- MOVER CARD ---");
        System.out.println("Cards disponíveis para movimentação:");

        int cardIndex = 1;
        for (Card card : cards) {
            String status = card.isBlocked() ? " [BLOQUEADO]" : "";
            String currentColumn = card.getColumn().getName();
            System.out.printf("%d. %s%s (Coluna atual: %s)%n",
                    cardIndex, card.getTitle(), status, currentColumn);
            cardIndex++;
        }

        System.out.print("\nSelecione o número do card: ");
        String cardChoice = scanner.nextLine().trim();

        try {
            int index = Integer.parseInt(cardChoice) - 1;
            if (index >= 0 && index < cards.size()) {
                Card card = cards.get(index);

                if (card.isBlocked()) {
                    System.out.println("Este card está bloqueado e não pode ser movido.");
                    return;
                }

                BoardColumn currentColumn = card.getColumn();
                BoardColumn nextColumn = board.getNextColumn(currentColumn);

                if (nextColumn == null) {
                    System.out.println("Este card já está na última coluna do fluxo.");
                    return;
                }

                System.out.print("Mover '" + card.getTitle() + "' para '" + nextColumn.getName() + "'? (s/n): ");
                String confirm = scanner.nextLine().trim().toLowerCase();

                if ("s".equals(confirm)) {
                    cardService.moveCard(card.getId(), null);
                    System.out.println("Card movido com sucesso!");
                }
            } else {
                System.out.println("Seleção inválida!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    /**
     * Cancela um card movendo para a coluna de cancelamento.
     *
     * @param board Board onde o card está
     */
    private void cancelCard(Board board) {
        List<Card> cards = cardService.listCardsByBoard(board.getId());

        if (cards.isEmpty()) {
            System.out.println("\nNenhum card encontrado no board.");
            return;
        }

        System.out.println("\n--- CANCELAR CARD ---");
        int cardIndex = 1;
        for (Card card : cards) {
            String status = card.isBlocked() ? " [BLOQUEADO]" : "";
            String currentColumn = card.getColumn().getName();
            System.out.printf("%d. %s%s (Coluna: %s)%n",
                    cardIndex, card.getTitle(), status, currentColumn);
            cardIndex++;
        }

        System.out.print("\nSelecione o número do card para cancelar: ");
        String cardChoice = scanner.nextLine().trim();

        try {
            int index = Integer.parseInt(cardChoice) - 1;
            if (index >= 0 && index < cards.size()) {
                Card card = cards.get(index);

                if (card.isBlocked()) {
                    System.out.println("Este card está bloqueado e não pode ser cancelado.");
                    return;
                }

                if (card.getColumn().isFinal()) {
                    System.out.println("Não é possível cancelar um card que já está na coluna final.");
                    return;
                }

                System.out.print("Cancelar o card '" + card.getTitle() + "'? (s/n): ");
                String confirm = scanner.nextLine().trim().toLowerCase();

                if ("s".equals(confirm)) {
                    cardService.cancelCard(card.getId());
                    System.out.println("Card cancelado com sucesso!");
                }
            } else {
                System.out.println("Seleção inválida!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    /**
     * Bloqueia um card com justificativa.
     *
     * @param board Board onde o card está
     */
    private void blockCard(Board board) {
        List<Card> cards = cardService.listCardsByBoard(board.getId());

        if (cards.isEmpty()) {
            System.out.println("\nNenhum card encontrado no board.");
            return;
        }

        System.out.println("\n--- BLOQUEAR CARD ---");
        int cardIndex = 1;
        for (Card card : cards) {
            String status = card.isBlocked() ? " [BLOQUEADO]" : "";
            System.out.printf("%d. %s%s%n", cardIndex, card.getTitle(), status);
            cardIndex++;
        }

        System.out.print("\nSelecione o número do card para bloquear: ");
        String cardChoice = scanner.nextLine().trim();

        try {
            int index = Integer.parseInt(cardChoice) - 1;
            if (index >= 0 && index < cards.size()) {
                Card card = cards.get(index);

                if (card.isBlocked()) {
                    System.out.println("Este card já está bloqueado.");
                    return;
                }

                System.out.print("Motivo do bloqueio: ");
                String reason = scanner.nextLine().trim();

                if (reason.isEmpty()) {
                    System.out.println("O motivo do bloqueio não pode ser vazio.");
                    return;
                }

                cardService.blockCard(card.getId(), reason);
                System.out.println("Card bloqueado com sucesso!");
            } else {
                System.out.println("Seleção inválida!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    /**
     * Desbloqueia um card com justificativa.
     *
     * @param board Board onde o card está
     */
    private void unblockCard(Board board) {
        List<Card> cards = cardService.listCardsByBoard(board.getId());

        if (cards.isEmpty()) {
            System.out.println("\nNenhum card encontrado no board.");
            return;
        }

        System.out.println("\n--- DESBLOQUEAR CARD ---");
        List<Card> blockedCards = cards.stream()
                .filter(Card::isBlocked)
                .toList();

        if (blockedCards.isEmpty()) {
            System.out.println("Nenhum card bloqueado encontrado.");
            return;
        }

        int cardIndex = 1;
        for (Card card : blockedCards) {
            System.out.printf("%d. %s [BLOQUEADO]%n", cardIndex, card.getTitle());
            cardIndex++;
        }

        System.out.print("\nSelecione o número do card para desbloquear: ");
        String cardChoice = scanner.nextLine().trim();

        try {
            int index = Integer.parseInt(cardChoice) - 1;
            if (index >= 0 && index < blockedCards.size()) {
                Card card = blockedCards.get(index);

                System.out.print("Motivo do desbloqueio: ");
                String reason = scanner.nextLine().trim();

                if (reason.isEmpty()) {
                    System.out.println("O motivo do desbloqueio não pode ser vazio.");
                    return;
                }

                cardService.unblockCard(card.getId(), reason);
                System.out.println("Card desbloqueado com sucesso!");
            } else {
                System.out.println("Seleção inválida!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    /**
     * Gera relatório de tempo de conclusão.
     *
     * @param board Board para gerar relatório
     */
    private void generateCompletionReport(Board board) {
        System.out.println("\n" + reportService.generateCompletionTimeReport(board.getId()));
    }

    /**
     * Gera relatório de bloqueios.
     *
     * @param board Board para gerar relatório
     */
    private void generateBlockadeReport(Board board) {
        System.out.println("\n" + reportService.generateBlockadeReport(board.getId()));
    }

    /**
     * Adiciona uma coluna pendente ao board.
     *
     * @param board Board para adicionar coluna
     */
    private void addPendingColumn(Board board) {
        System.out.println("\n--- ADICIONAR COLUNA PENDENTE ---");
        System.out.print("Nome da nova coluna: ");
        String columnName = scanner.nextLine().trim();

        boardService.addPendingColumn(board.getId(), columnName);
        System.out.println("Coluna adicionada com sucesso!");
    }
}
