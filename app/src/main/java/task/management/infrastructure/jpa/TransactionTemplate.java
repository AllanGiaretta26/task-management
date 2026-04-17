package task.management.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Template de transação para executar blocos de código sob uma transação JPA.
 *
 * Centraliza o padrão begin/commit/rollback, eliminando a duplicação que existia
 * nos services. Em caso de qualquer {@link RuntimeException}, efetua rollback
 * automático e propaga a exceção.
 *
 * <p>Exemplo de uso:
 * <pre>{@code
 * TransactionTemplate tx = new TransactionTemplate(entityManager);
 * Card saved = tx.execute(() -> cardRepository.save(card));
 * }</pre>
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public class TransactionTemplate {

    private final EntityManager entityManager;

    public TransactionTemplate(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Executa a ação dentro de uma transação e retorna o resultado.
     *
     * @param action Ação a ser executada
     * @param <T>    Tipo de retorno
     * @return Resultado da ação
     */
    public <T> T execute(Supplier<T> action) {
        EntityTransaction tx = entityManager.getTransaction();
        tx.begin();
        try {
            T result = action.get();
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Executa a ação dentro de uma transação sem retorno.
     *
     * @param action Ação a ser executada
     */
    public void executeVoid(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Executa a ação dentro de uma transação, traduzindo qualquer RuntimeException
     * em uma exceção de domínio do chamador.
     *
     * @param action           Ação a ser executada
     * @param exceptionWrapper Função que converte a exceção original
     * @param <T>              Tipo de retorno
     * @return Resultado da ação
     */
    public <T> T execute(Supplier<T> action, Function<Exception, RuntimeException> exceptionWrapper) {
        try {
            return execute(action);
        } catch (RuntimeException e) {
            throw exceptionWrapper.apply(e);
        }
    }
}
