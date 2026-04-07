package task.management.infrastructure.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import task.management.domain.Blockade;
import task.management.domain.Card;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para operações de persistência da entidade Blockade.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class BlockadeRepository {

    private final EntityManager entityManager;

    /**
     * Construtor que recebe o EntityManager.
     *
     * @param entityManager EntityManager para persistência
     */
    public BlockadeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Salva um registro de bloqueio no banco de dados.
     *
     * @param blockade Registro de bloqueio a ser salvo
     * @return Registro salvo com ID preenchido
     */
    public Blockade save(Blockade blockade) {
        if (blockade.getId() == null) {
            entityManager.persist(blockade);
            entityManager.flush();
            return blockade;
        }
        return entityManager.merge(blockade);
    }

    /**
     * Lista todos os bloqueios de um card.
     *
     * @param card Card para buscar os bloqueios
     * @return Lista de bloqueios ordenados por data
     */
    public List<Blockade> findByCard(Card card) {
        TypedQuery<Blockade> query = entityManager.createQuery(
                "SELECT b FROM Blockade b WHERE b.card = :card ORDER BY b.blockedAt", Blockade.class);
        query.setParameter("card", card);
        return query.getResultList();
    }

    /**
     * Busca bloqueios de um card em um período específico.
     *
     * @param card Card para buscar os bloqueios
     * @param startDate Data de início do período
     * @param endDate Data de fim do período
     * @return Lista de bloqueios no período
     */
    public List<Blockade> findByCardAndPeriod(Card card, LocalDateTime startDate, LocalDateTime endDate) {
        TypedQuery<Blockade> query = entityManager.createQuery(
                "SELECT b FROM Blockade b WHERE b.card = :card AND b.blockedAt BETWEEN :startDate AND :endDate ORDER BY b.blockedAt",
                Blockade.class);
        query.setParameter("card", card);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }
}
