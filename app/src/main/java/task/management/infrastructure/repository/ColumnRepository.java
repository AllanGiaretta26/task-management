package task.management.infrastructure.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import task.management.domain.BoardColumn;
import task.management.domain.ColumnType;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações de persistência da entidade BoardColumn.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class ColumnRepository {

    private final EntityManager entityManager;

    /**
     * Construtor que recebe o EntityManager.
     *
     * @param entityManager EntityManager para persistência
     */
    public ColumnRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Salva ou atualiza uma coluna no banco de dados.
     *
     * @param column Coluna a ser salva
     * @return Coluna salva com ID preenchido
     */
    public BoardColumn save(BoardColumn column) {
        if (column.getId() == null) {
            entityManager.persist(column);
            entityManager.flush();
            return column;
        }
        return entityManager.merge(column);
    }

    /**
     * Busca uma coluna pelo seu ID.
     *
     * @param id ID da coluna
     * @return Optional contendo a coluna se encontrada
     */
    public Optional<BoardColumn> findById(Long id) {
        BoardColumn column = entityManager.find(BoardColumn.class, id);
        return Optional.ofNullable(column);
    }

    /**
     * Lista todas as colunas de um board.
     *
     * @param boardId ID do board
     * @return Lista de colunas ordenadas por posição
     */
    public List<BoardColumn> findByBoardId(Long boardId) {
        TypedQuery<BoardColumn> query = entityManager.createQuery(
                "SELECT c FROM BoardColumn c WHERE c.board.id = :boardId ORDER BY c.position", BoardColumn.class);
        query.setParameter("boardId", boardId);
        return query.getResultList();
    }

    /**
     * Busca uma coluna pelo tipo em um board específico.
     *
     * @param boardId ID do board
     * @param type Tipo da coluna
     * @return Optional contendo a coluna se encontrada
     */
    public Optional<BoardColumn> findByBoardIdAndType(Long boardId, ColumnType type) {
        TypedQuery<BoardColumn> query = entityManager.createQuery(
                "SELECT c FROM BoardColumn c WHERE c.board.id = :boardId AND c.type = :type", BoardColumn.class);
        query.setParameter("boardId", boardId);
        query.setParameter("type", type);
        return query.getResultList().stream().findFirst();
    }

    /**
     * Remove uma coluna do banco de dados.
     *
     * @param column Coluna a ser removida
     */
    public void delete(BoardColumn column) {
        BoardColumn managedColumn = entityManager.contains(column) ? column : entityManager.merge(column);
        entityManager.remove(managedColumn);
    }
}
