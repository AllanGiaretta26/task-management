package task.management.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import task.management.domain.Board;
import task.management.domain.BoardColumn;
import task.management.domain.ColumnType;
import task.management.infrastructure.jpa.TransactionTemplate;
import task.management.infrastructure.repository.BoardRepository;

import java.util.List;

/**
 * Serviço responsável pelas regras de negócio relacionadas aos boards.
 *
 * Cada operação abre seu próprio {@link EntityManager} via try-with-resources,
 * força o carregamento do grafo lazy e retorna a entidade detached — a camada
 * de UI pode navegar o grafo sem risco de {@code LazyInitializationException}.
 *
 * @author Allan Giaretta
 * @version 2.0
 */
public class BoardService {

    private final EntityManagerFactory emf;

    /**
     * Construtor do serviço de board.
     *
     * @param entityManagerFactory EntityManagerFactory compartilhado
     */
    public BoardService(EntityManagerFactory entityManagerFactory) {
        this.emf = entityManagerFactory;
    }

    /**
     * Cria um novo board com a estrutura mínima de colunas (inicial, final, cancelada).
     *
     * @param boardName Nome do board
     * @param initialColumnName Nome da coluna inicial
     * @param finalColumnName Nome da coluna final
     * @param cancelledColumnName Nome da coluna de cancelamento
     * @return Board criado (detached, com grafo inflado)
     */
    public Board createBoard(String boardName, String initialColumnName,
                             String finalColumnName, String cancelledColumnName) {
        validateBoardName(boardName);

        Board board = new Board(boardName);
        board.addColumn(new BoardColumn(initialColumnName, ColumnType.INITIAL, 0));
        board.addColumn(new BoardColumn(finalColumnName, ColumnType.FINAL, 1));
        board.addColumn(new BoardColumn(cancelledColumnName, ColumnType.CANCELLED, 2));

        try (EntityManager em = emf.createEntityManager()) {
            BoardRepository repo = new BoardRepository(em);
            TransactionTemplate tx = new TransactionTemplate(em);
            Board saved = tx.execute(
                    () -> repo.save(board),
                    e -> new BoardServiceException("Erro ao criar board: " + e.getMessage(), e));
            repo.initializeGraph(saved);
            return saved;
        }
    }

    /**
     * Adiciona uma coluna pendente ao board, inserindo-a antes da coluna final.
     *
     * @param boardId ID do board
     * @param columnName Nome da coluna
     * @return Board atualizado (detached, com grafo inflado)
     */
    public Board addPendingColumn(Long boardId, String columnName) {
        validateColumnName(columnName);

        try (EntityManager em = emf.createEntityManager()) {
            BoardRepository repo = new BoardRepository(em);
            TransactionTemplate tx = new TransactionTemplate(em);

            Board saved = tx.execute(() -> {
                Board board = repo.findById(boardId)
                        .orElseThrow(() -> new BoardServiceException("Board não encontrado com ID: " + boardId));

                BoardColumn finalColumn = board.findColumnByType(ColumnType.FINAL);
                if (finalColumn == null) {
                    throw new BoardServiceException("Board não possui coluna final definida");
                }

                int insertPosition = finalColumn.getPosition();
                board.getColumns().stream()
                        .filter(c -> c.getPosition() >= insertPosition && c.getType() != ColumnType.CANCELLED)
                        .forEach(c -> c.setPosition(c.getPosition() + 1));

                board.addColumn(new BoardColumn(columnName, ColumnType.PENDING, insertPosition));
                return repo.save(board);
            }, e -> (e instanceof BoardServiceException bse)
                    ? bse
                    : new BoardServiceException("Erro ao adicionar coluna: " + e.getMessage(), e));

            repo.initializeGraph(saved);
            return saved;
        }
    }

    /**
     * Lista todos os boards cadastrados (com grafo inflado).
     *
     * @return Lista de boards detached
     */
    public List<Board> listAllBoards() {
        try (EntityManager em = emf.createEntityManager()) {
            BoardRepository repo = new BoardRepository(em);
            List<Board> boards = repo.findAll();
            repo.initializeGraph(boards);
            return boards;
        }
    }

    /**
     * Busca um board pelo ID (com grafo inflado).
     *
     * @param boardId ID do board
     * @return Board encontrado (detached)
     */
    public Board findBoardById(Long boardId) {
        try (EntityManager em = emf.createEntityManager()) {
            BoardRepository repo = new BoardRepository(em);
            Board board = repo.findById(boardId)
                    .orElseThrow(() -> new BoardServiceException("Board não encontrado com ID: " + boardId));
            repo.initializeGraph(board);
            return board;
        }
    }

    /**
     * Remove um board pelo ID.
     *
     * @param boardId ID do board
     */
    public void deleteBoard(Long boardId) {
        try (EntityManager em = emf.createEntityManager()) {
            BoardRepository repo = new BoardRepository(em);
            TransactionTemplate tx = new TransactionTemplate(em);
            tx.execute(() -> {
                Board board = repo.findById(boardId)
                        .orElseThrow(() -> new BoardServiceException("Board não encontrado com ID: " + boardId));
                repo.delete(board);
                return null;
            }, e -> (e instanceof BoardServiceException bse)
                    ? bse
                    : new BoardServiceException("Erro ao excluir board: " + e.getMessage(), e));
        }
    }

    private void validateBoardName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BoardServiceException("O nome do board não pode ser vazio");
        }
        if (name.length() > 100) {
            throw new BoardServiceException("O nome do board deve ter no máximo 100 caracteres");
        }
    }

    private void validateColumnName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BoardServiceException("O nome da coluna não pode ser vazio");
        }
        if (name.length() > 100) {
            throw new BoardServiceException("O nome da coluna deve ter no máximo 100 caracteres");
        }
    }
}
