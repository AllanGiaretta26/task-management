package task.management.service;

import jakarta.persistence.EntityManager;
import task.management.domain.Board;
import task.management.domain.BoardColumn;
import task.management.domain.Card;
import task.management.domain.ColumnType;
import task.management.infrastructure.repository.BoardRepository;
import task.management.infrastructure.repository.CardRepository;

import java.util.List;

/**
 * Serviço responsável pelas regras de negócio relacionadas aos cards.
 *
 * Implementa regras de movimentação entre colunas, bloqueio/desbloqueio
 * e validações de negócio.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class CardService {

    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final EntityManager entityManager;

    /**
     * Construtor do serviço de card.
     *
     * @param entityManager EntityManager para persistência
     */
    public CardService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.boardRepository = new BoardRepository(entityManager);
        this.cardRepository = new CardRepository(entityManager);
    }

    /**
     * Cria um novo card na coluna inicial do board.
     *
     * @param boardId ID do board
     * @param title Título do card
     * @param description Descrição do card
     * @return Card criado
     */
    public Card createCard(Long boardId, String title, String description) {
        Board board = findBoardOrThrow(boardId);
        BoardColumn initialColumn = board.findColumnByType(ColumnType.INITIAL);

        if (initialColumn == null) {
            throw new CardServiceException("Board não possui coluna inicial definida");
        }

        Card card = new Card(title, description);
        initialColumn.addCard(card);
        card.recordColumnEntry(initialColumn);

        entityManager.getTransaction().begin();
        try {
            cardRepository.save(card);
            entityManager.getTransaction().commit();
            return card;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new CardServiceException("Erro ao criar card: " + e.getMessage(), e);
        }
    }

    /**
     * Move um card para a próxima coluna no fluxo do board.
     *
     * Regras:
     * - Cards devem seguir a ordem das colunas sem pular etapas
     * - Cards bloqueados não podem ser movidos
     * - Cards de qualquer coluna (exceto final) podem ser movidos para cancelada
     *
     * @param cardId ID do card
     * @param targetColumnId ID da coluna destino (opcional, se null move para próxima)
     * @return Card movido
     */
    public Card moveCard(Long cardId, Long targetColumnId) {
        Card card = findCardOrThrow(cardId);

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
            validateMoveToColumn(card, currentColumn, targetColumn);
        } else {
            targetColumn = board.getNextColumn(currentColumn);
            if (targetColumn == null) {
                throw new CardServiceException("Não há próxima coluna para mover o card");
            }
        }

        // Remove da coluna atual e adiciona na destino
        currentColumn.removeCard(card);
        targetColumn.addCard(card);
        card.recordColumnEntry(targetColumn);

        entityManager.getTransaction().begin();
        try {
            cardRepository.save(card);
            entityManager.getTransaction().commit();
            return card;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new CardServiceException("Erro ao mover card: " + e.getMessage(), e);
        }
    }

    /**
     * Move um card diretamente para a coluna de cancelamento.
     *
     * @param cardId ID do card
     * @return Card cancelado
     */
    public Card cancelCard(Long cardId) {
        Card card = findCardOrThrow(cardId);

        if (card.isBlocked()) {
            throw new CardServiceException("Não é possível cancelar um card bloqueado. Desbloqueie primeiro.");
        }

        BoardColumn currentColumn = card.getColumn();
        Board board = currentColumn.getBoard();

        BoardColumn cancelledColumn = board.findColumnByType(ColumnType.CANCELLED);
        if (cancelledColumn == null) {
            throw new CardServiceException("Board não possui coluna de cancelamento definida");
        }

        // Não permite cancelar se já estiver na coluna final
        if (currentColumn.isFinal()) {
            throw new CardServiceException("Não é possível cancelar um card que já está na coluna final");
        }

        currentColumn.removeCard(card);
        cancelledColumn.addCard(card);
        card.recordColumnEntry(cancelledColumn);

        entityManager.getTransaction().begin();
        try {
            cardRepository.save(card);
            entityManager.getTransaction().commit();
            return card;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new CardServiceException("Erro ao cancelar card: " + e.getMessage(), e);
        }
    }

    /**
     * Bloqueia um card com uma justificativa.
     *
     * @param cardId ID do card
     * @param reason Motivo do bloqueio
     * @return Card bloqueado
     */
    public Card blockCard(Long cardId, String reason) {
        Card card = findCardOrThrow(cardId);
        card.block(reason);

        entityManager.getTransaction().begin();
        try {
            cardRepository.save(card);
            entityManager.getTransaction().commit();
            return card;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new CardServiceException("Erro ao bloquear card: " + e.getMessage(), e);
        }
    }

    /**
     * Desbloqueia um card com uma justificativa.
     *
     * @param cardId ID do card
     * @param reason Motivo do desbloqueio
     * @return Card desbloqueado
     */
    public Card unblockCard(Long cardId, String reason) {
        Card card = findCardOrThrow(cardId);
        card.unblock(reason);

        entityManager.getTransaction().begin();
        try {
            cardRepository.save(card);
            entityManager.getTransaction().commit();
            return card;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new CardServiceException("Erro ao desbloquear card: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos os cards de um board.
     *
     * @param boardId ID do board
     * @return Lista de cards
     */
    public List<Card> listCardsByBoard(Long boardId) {
        findBoardOrThrow(boardId);
        return cardRepository.findByBoardId(boardId);
    }

    /**
     * Busca um card pelo ID.
     *
     * @param cardId ID do card
     * @return Card encontrado
     */
    public Card findCardById(Long cardId) {
        return findCardOrThrow(cardId);
    }

    /**
     * Valida se a movimentação do card para a coluna destino é válida.
     *
     * @param card Card sendo movido
     * @param currentColumn Coluna atual
     * @param targetColumn Coluna destino
     */
    private void validateMoveToColumn(Card card, BoardColumn currentColumn, BoardColumn targetColumn) {
        // Pode mover para coluna de cancelamento de qualquer lugar (exceto final)
        if (targetColumn.isCancelled()) {
            if (currentColumn.isFinal()) {
                throw new CardServiceException("Não é possível mover um card da coluna final para cancelada");
            }
            return;
        }

        // Verifica se a coluna destino é a próxima na sequência
        Board board = currentColumn.getBoard();
        BoardColumn nextColumn = board.getNextColumn(currentColumn);

        if (nextColumn == null || !nextColumn.getId().equals(targetColumn.getId())) {
            throw new CardServiceException("Não é possível pular colunas. Mova o card para a próxima coluna na sequência.");
        }
    }

    /**
     * Busca um card ou lança exceção se não encontrado.
     *
     * @param cardId ID do card
     * @return Card encontrado
     */
    private Card findCardOrThrow(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardServiceException("Card não encontrado com ID: " + cardId));
    }

    /**
     * Busca um board ou lança exceção se não encontrado.
     *
     * @param boardId ID do board
     * @return Board encontrado
     */
    private Board findBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CardServiceException("Board não encontrado com ID: " + boardId));
    }
}
