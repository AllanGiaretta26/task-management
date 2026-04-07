package task.management.infrastructure.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import task.management.domain.Board;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações de persistência da entidade Board.
 *
 * Implementa o padrão Repository para encapsular o acesso a dados
 * e fornecer uma interface limpa para a camada de serviço.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class BoardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Construtor que recebe o EntityManager.
     *
     * @param entityManager EntityManager para persistência
     */
    public BoardRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Salva ou atualiza um board no banco de dados.
     *
     * @param board Board a ser salvo
     * @return Board salvo com ID preenchido
     */
    public Board save(Board board) {
        if (board.getId() == null) {
            entityManager.persist(board);
            entityManager.flush();
            return board;
        }
        return entityManager.merge(board);
    }

    /**
     * Busca um board pelo seu ID.
     *
     * @param id ID do board
     * @return Optional contendo o board se encontrado
     */
    public Optional<Board> findById(Long id) {
        Board board = entityManager.find(Board.class, id);
        return Optional.ofNullable(board);
    }

    /**
     * Lista todos os boards cadastrados.
     *
     * @return Lista de boards
     */
    public List<Board> findAll() {
        TypedQuery<Board> query = entityManager.createQuery(
                "SELECT DISTINCT b FROM Board b ORDER BY b.name", Board.class);
        return query.getResultList();
    }

    /**
     * Busca um board pelo nome.
     *
     * @param name Nome do board
     * @return Optional contendo o board se encontrado
     */
    public Optional<Board> findByName(String name) {
        TypedQuery<Board> query = entityManager.createQuery(
                "SELECT b FROM Board b WHERE b.name = :name", Board.class);
        query.setParameter("name", name);
        return query.getResultList().stream().findFirst();
    }

    /**
     * Remove um board do banco de dados.
     *
     * @param board Board a ser removido
     */
    public void delete(Board board) {
        Board managedBoard = entityManager.contains(board) ? board : entityManager.merge(board);
        entityManager.remove(managedBoard);
    }

    /**
     * Remove um board pelo ID.
     *
     * @param id ID do board a ser removido
     */
    public void deleteById(Long id) {
        findById(id).ifPresent(this::delete);
    }
}
