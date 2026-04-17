package task.management.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import task.management.domain.Blockade;
import task.management.domain.Board;
import task.management.domain.BoardColumn;
import task.management.domain.Card;
import task.management.domain.ColumnHistory;
import task.management.domain.ColumnType;
import task.management.infrastructure.jpa.TransactionTemplate;
import task.management.infrastructure.repository.BoardRepository;
import task.management.infrastructure.repository.CardRepository;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Serviço responsável pelas regras de negócio relacionadas aos cards.
 *
 * Cada operação abre seu próprio {@link EntityManager} via try-with-resources
 * e força a inflação do grafo do card (coluna + bloqueios + histórico) antes
 * de fechar o EM, retornando a entidade detached.
 *
 * @author Allan Giaretta
 * @version 4.0
 */
public class CardService {

    private final EntityManagerFactory emf;

    /**
     * Construtor do serviço de card.
     *
     * @param entityManagerFactory EntityManagerFactory compartilhado
     */
    public CardService(EntityManagerFactory entityManagerFactory) {
        this.emf = entityManagerFactory;
    }

    /**
     * Cria um novo card na coluna inicial do board.
     *
     * @param boardId ID do board
     * @param title Título do card
     * @param description Descrição do card
     * @return Card criado (detached, com coluna e histórico carregados)
     */
    public Card createCard(Long boardId, String title, String description) {
        return executeInTransaction((em, cardRepo) -> {
            BoardRepository boardRepo = new BoardRepository(em);
            Board board = boardRepo.findById(boardId)
                    .orElseThrow(() -> new CardServiceException("Board não encontrado com ID: " + boardId));

            BoardColumn initialColumn = board.findColumnByType(ColumnType.INITIAL);
            if (initialColumn == null) {
                throw new CardServiceException("Board não possui coluna inicial definida");
            }

            Card card = new Card(title, description);
            initialColumn.addCard(card);
            card.recordColumnEntry(initialColumn);
            return cardRepo.save(card);
        }, "Erro ao criar card");
    }

    /**
     * Move um card para a próxima coluna no fluxo do board.
     *
     * @param cardId ID do card
     * @param targetColumnId ID da coluna destino (opcional — null move para a próxima)
     * @return Card movido (detached)
     */
    public Card moveCard(Long cardId, Long targetColumnId) {
        return executeInTransaction((em, cardRepo) -> {
            Card card = cardRepo.findById(cardId)
                    .orElseThrow(() -> new CardServiceException("Card não encontrado com ID: " + cardId));

            if (card.isBlocked()) {
                throw new CardServiceException("Não é possível mover um card bloqueado. Desbloqueie primeiro.");
            }

            BoardColumn currentColumn = card.getColumn();
            Board board = currentColumn.getBoard();

            BoardColumn targetColumn;
            if (targetColumnId != null) {
                targetColumn = board.findColumnById(targetColumnId);
                if (targetColumn == null) {
                    throw new CardServiceException("Coluna destino não encontrada");
                }
                validateMoveToColumn(currentColumn, targetColumn);
            } else {
                targetColumn = board.getNextColumn(currentColumn);
                if (targetColumn == null) {
                    throw new CardServiceException("Não há próxima coluna para mover o card");
                }
            }

            currentColumn.removeCard(card);
            targetColumn.addCard(card);
            card.recordColumnEntry(targetColumn);
            return cardRepo.save(card);
        }, "Erro ao mover card");
    }

    /**
     * Move um card diretamente para a coluna de cancelamento.
     *
     * @param cardId ID do card
     * @return Card cancelado (detached)
     */
    public Card cancelCard(Long cardId) {
        return executeInTransaction((em, cardRepo) -> {
            Card card = cardRepo.findById(cardId)
                    .orElseThrow(() -> new CardServiceException("Card não encontrado com ID: " + cardId));

            if (card.isBlocked()) {
                throw new CardServiceException("Não é possível cancelar um card bloqueado. Desbloqueie primeiro.");
            }

            BoardColumn currentColumn = card.getColumn();
            Board board = currentColumn.getBoard();

            BoardColumn cancelledColumn = board.findColumnByType(ColumnType.CANCELLED);
            if (cancelledColumn == null) {
                throw new CardServiceException("Board não possui coluna de cancelamento definida");
            }

            if (currentColumn.isFinal()) {
                throw new CardServiceException("Não é possível cancelar um card que já está na coluna final");
            }

            currentColumn.removeCard(card);
            cancelledColumn.addCard(card);
            card.recordColumnEntry(cancelledColumn);
            return cardRepo.save(card);
        }, "Erro ao cancelar card");
    }

    /**
     * Bloqueia um card com uma justificativa.
     *
     * @param cardId ID do card
     * @param reason Motivo do bloqueio
     * @return Card bloqueado (detached)
     */
    public Card blockCard(Long cardId, String reason) {
        return executeInTransaction((em, cardRepo) -> {
            Card card = cardRepo.findById(cardId)
                    .orElseThrow(() -> new CardServiceException("Card não encontrado com ID: " + cardId));
            card.block(reason);
            return cardRepo.save(card);
        }, "Erro ao bloquear card");
    }

    /**
     * Desbloqueia um card com uma justificativa.
     *
     * @param cardId ID do card
     * @param reason Motivo do desbloqueio
     * @return Card desbloqueado (detached)
     */
    public Card unblockCard(Long cardId, String reason) {
        return executeInTransaction((em, cardRepo) -> {
            Card card = cardRepo.findById(cardId)
                    .orElseThrow(() -> new CardServiceException("Card não encontrado com ID: " + cardId));
            card.unblock(reason);
            return cardRepo.save(card);
        }, "Erro ao desbloquear card");
    }

    /**
     * Lista todos os cards de um board (com coluna e histórico carregados).
     *
     * @param boardId ID do board
     * @return Lista de cards detached
     */
    public List<Card> listCardsByBoard(Long boardId) {
        try (EntityManager em = emf.createEntityManager()) {
            BoardRepository boardRepo = new BoardRepository(em);
            if (boardRepo.findById(boardId).isEmpty()) {
                throw new CardServiceException("Board não encontrado com ID: " + boardId);
            }
            CardRepository cardRepo = new CardRepository(em);
            List<Card> cards = cardRepo.findByBoardId(boardId);
            cards.forEach(this::initializeCardGraph);
            return cards;
        }
    }

    /**
     * Busca um card pelo ID (com grafo inflado).
     *
     * @param cardId ID do card
     * @return Card encontrado (detached)
     */
    public Card findCardById(Long cardId) {
        try (EntityManager em = emf.createEntityManager()) {
            CardRepository cardRepo = new CardRepository(em);
            Card card = cardRepo.findById(cardId)
                    .orElseThrow(() -> new CardServiceException("Card não encontrado com ID: " + cardId));
            initializeCardGraph(card);
            return card;
        }
    }

    @FunctionalInterface
    private interface CardOperation {
        Card run(EntityManager em, CardRepository cardRepo);
    }

    private Card executeInTransaction(CardOperation operation, String errorPrefix) {
        try (EntityManager em = emf.createEntityManager()) {
            CardRepository cardRepo = new CardRepository(em);
            TransactionTemplate tx = new TransactionTemplate(em);
            Function<Exception, RuntimeException> wrapper = e -> (e instanceof CardServiceException cse)
                    ? cse
                    : new CardServiceException(errorPrefix + ": " + e.getMessage(), e);
            Supplier<Card> action = () -> operation.run(em, cardRepo);
            Card saved = tx.execute(action, wrapper);
            initializeCardGraph(saved);
            return saved;
        }
    }

    private void initializeCardGraph(Card card) {
        if (card == null) {
            return;
        }
        BoardColumn column = card.getColumn();
        if (column != null) {
            column.getName();
            column.getType();
        }
        for (Blockade blockade : card.getBlockades()) {
            blockade.getReason();
        }
        for (ColumnHistory entry : card.getColumnHistory()) {
            if (entry.getColumn() != null) {
                entry.getColumn().getName();
            }
        }
    }

    private void validateMoveToColumn(BoardColumn currentColumn, BoardColumn targetColumn) {
        if (targetColumn.isCancelled()) {
            if (currentColumn.isFinal()) {
                throw new CardServiceException("Não é possível mover um card da coluna final para cancelada");
            }
            return;
        }

        Board board = currentColumn.getBoard();
        BoardColumn nextColumn = board.getNextColumn(currentColumn);

        if (nextColumn == null || !nextColumn.getId().equals(targetColumn.getId())) {
            throw new CardServiceException("Não é possível pular colunas. Mova o card para a próxima coluna na sequência.");
        }
    }
}
