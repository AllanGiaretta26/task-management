package task.management.service;

import jakarta.persistence.EntityManager;
import task.management.domain.Board;
import task.management.domain.BoardColumn;
import task.management.domain.ColumnType;
import task.management.infrastructure.repository.BoardRepository;

import java.util.List;

/**
 * Serviço responsável pelas regras de negócio relacionadas aos boards.
 *
 * Implementa o padrão Service para encapsular a lógica de negócio
 * e fornecer uma interface limpa para a camada de apresentação.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class BoardService {

    private final BoardRepository boardRepository;
    private final EntityManager entityManager;

    /**
     * Construtor do serviço de board.
     *
     * @param entityManager EntityManager para persistência
     */
    public BoardService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.boardRepository = new BoardRepository(entityManager);
    }

    /**
     * Cria um novo board com a estrutura mínima de colunas.
     *
     * Um board deve ter pelo menos 3 colunas: inicial, final e cancelada.
     * A ordem obrigatória é: inicial (primeira), pendente (intermediárias),
     * final (penúltima), cancelada (última).
     *
     * @param boardName Nome do board
     * @param initialColumnName Nome da coluna inicial
     * @param finalColumnName Nome da coluna final
     * @param cancelledColumnName Nome da coluna de cancelamento
     * @return Board criado
     */
    public Board createBoard(String boardName, String initialColumnName,
                             String finalColumnName, String cancelledColumnName) {
        validateBoardName(boardName);

        Board board = new Board(boardName);

        // Cria coluna inicial (posição 0)
        BoardColumn initialColumn = new BoardColumn(initialColumnName, ColumnType.INITIAL, 0);
        board.addColumn(initialColumn);

        // Cria coluna final (posição 1 - penúltima na estrutura mínima)
        BoardColumn finalColumn = new BoardColumn(finalColumnName, ColumnType.FINAL, 1);
        board.addColumn(finalColumn);

        // Cria coluna de cancelamento (posição 2 - última)
        BoardColumn cancelledColumn = new BoardColumn(cancelledColumnName, ColumnType.CANCELLED, 2);
        board.addColumn(cancelledColumn);

        entityManager.getTransaction().begin();
        try {
            boardRepository.save(board);
            entityManager.getTransaction().commit();
            return board;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new BoardServiceException("Erro ao criar board: " + e.getMessage(), e);
        }
    }

    /**
     * Adiciona uma coluna pendente ao board.
     *
     * Colunas pendentes são inseridas antes da coluna final.
     *
     * @param boardId ID do board
     * @param columnName Nome da coluna
     * @return Board atualizado
     */
    public Board addPendingColumn(Long boardId, String columnName) {
        validateColumnName(columnName);

        Board board = findBoardOrThrow(boardId);

        // Encontra a posição da coluna final
        BoardColumn finalColumn = board.findColumnByType(ColumnType.FINAL);
        if (finalColumn == null) {
            throw new BoardServiceException("Board não possui coluna final definida");
        }

        int insertPosition = finalColumn.getPosition();

        // Desloca as colunas a partir da posição de inserção
        board.getColumns().stream()
                .filter(c -> c.getPosition() >= insertPosition && c.getType() != ColumnType.CANCELLED)
                .forEach(c -> c.setPosition(c.getPosition() + 1));

        // Cria a nova coluna pendente
        BoardColumn pendingColumn = new BoardColumn(columnName, ColumnType.PENDING, insertPosition);
        board.addColumn(pendingColumn);

        entityManager.getTransaction().begin();
        try {
            boardRepository.save(board);
            entityManager.getTransaction().commit();
            return board;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new BoardServiceException("Erro ao adicionar coluna: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos os boards cadastrados.
     *
     * @return Lista de boards
     */
    public List<Board> listAllBoards() {
        return boardRepository.findAll();
    }

    /**
     * Busca um board pelo ID com colunas carregadas.
     *
     * @param boardId ID do board
     * @return Board encontrado
     */
    public Board findBoardById(Long boardId) {
        return findBoardOrThrow(boardId);
    }

    /**
     * Remove um board pelo ID.
     *
     * @param boardId ID do board
     */
    public void deleteBoard(Long boardId) {
        Board board = findBoardOrThrow(boardId);

        entityManager.getTransaction().begin();
        try {
            boardRepository.delete(board);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new BoardServiceException("Erro ao excluir board: " + e.getMessage(), e);
        }
    }

    /**
     * Valida se o nome do board é válido.
     *
     * @param name Nome do board
     */
    private void validateBoardName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BoardServiceException("O nome do board não pode ser vazio");
        }
        if (name.length() > 100) {
            throw new BoardServiceException("O nome do board deve ter no máximo 100 caracteres");
        }
    }

    /**
     * Valida se o nome da coluna é válido.
     *
     * @param name Nome da coluna
     */
    private void validateColumnName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BoardServiceException("O nome da coluna não pode ser vazio");
        }
        if (name.length() > 100) {
            throw new BoardServiceException("O nome da coluna deve ter no máximo 100 caracteres");
        }
    }

    /**
     * Busca um board ou lança exceção se não encontrado.
     *
     * @param boardId ID do board
     * @return Board encontrado
     */
    private Board findBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardServiceException("Board não encontrado com ID: " + boardId));
    }
}
