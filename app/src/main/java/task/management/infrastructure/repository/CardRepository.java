package task.management.infrastructure.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import task.management.domain.Card;
import task.management.domain.CardStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações de persistência da entidade Card.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class CardRepository {

    private final EntityManager entityManager;

    /**
     * Construtor que recebe o EntityManager.
     *
     * @param entityManager EntityManager para persistência
     */
    public CardRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Salva ou atualiza um card no banco de dados.
     *
     * @param card Card a ser salvo
     * @return Card salvo com ID preenchido
     */
    public Card save(Card card) {
        if (card.getId() == null) {
            entityManager.persist(card);
            entityManager.flush();
            return card;
        }
        // Se a entidade já está gerenciada, flush é suficiente
        if (entityManager.contains(card)) {
            entityManager.flush();
            return card;
        }
        return entityManager.merge(card);
    }

    /**
     * Busca um card pelo seu ID.
     *
     * @param id ID do card
     * @return Optional contendo o card se encontrado
     */
    public Optional<Card> findById(Long id) {
        Card card = entityManager.find(Card.class, id);
        return Optional.ofNullable(card);
    }

    /**
     * Lista todos os cards de uma coluna.
     *
     * @param columnId ID da coluna
     * @return Lista de cards ordenados por data de criação
     */
    public List<Card> findByColumnId(Long columnId) {
        TypedQuery<Card> query = entityManager.createQuery(
                "SELECT c FROM Card c WHERE c.column.id = :columnId ORDER BY c.createdAt", Card.class);
        query.setParameter("columnId", columnId);
        return query.getResultList();
    }

    /**
     * Lista todos os cards de um board.
     *
     * @param boardId ID do board
     * @return Lista de cards
     */
    public List<Card> findByBoardId(Long boardId) {
        TypedQuery<Card> query = entityManager.createQuery(
                "SELECT c FROM Card c WHERE c.column.board.id = :boardId ORDER BY c.createdAt", Card.class);
        query.setParameter("boardId", boardId);
        return query.getResultList();
    }

    /**
     * Busca cards por status em uma coluna específica.
     *
     * @param columnId ID da coluna
     * @param status Status do card
     * @return Lista de cards com o status especificado
     */
    public List<Card> findByColumnIdAndStatus(Long columnId, CardStatus status) {
        TypedQuery<Card> query = entityManager.createQuery(
                "SELECT c FROM Card c WHERE c.column.id = :columnId AND c.status = :status ORDER BY c.createdAt",
                Card.class);
        query.setParameter("columnId", columnId);
        query.setParameter("status", status);
        return query.getResultList();
    }

    /**
     * Remove um card do banco de dados.
     *
     * @param card Card a ser removido
     */
    public void delete(Card card) {
        Card managedCard = entityManager.contains(card) ? card : entityManager.merge(card);
        entityManager.remove(managedCard);
    }
}
