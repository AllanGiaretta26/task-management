package task.management.service;

/**
 * Exceção lançada quando ocorrem erros nas operações de serviço do board.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class BoardServiceException extends RuntimeException {

    /**
     * Construtor com mensagem de erro.
     *
     * @param message Mensagem de erro
     */
    public BoardServiceException(String message) {
        super(message);
    }

    /**
     * Construtor com mensagem e causa.
     *
     * @param message Mensagem de erro
     * @param cause Causa do erro
     */
    public BoardServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
