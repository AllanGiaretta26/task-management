package task.management.migrations;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço responsável por executar migrações do banco de dados usando Flyway.
 *
 * Esta classe encapsula a lógica de execução de migrações e fornece tratamento
 * de exceções específico, convertendo erros do Flyway em uma exceção customizada.
 *
 * @author Allan Giaretta
 * @version 2.0
 * @see MigrationService.MigrationException
 */
public class MigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationService.class);
    
    // Instância do Flyway injetada para executar as migrações
    private final Flyway flyway;

    /**
     * Construtor que recebe uma instância de Flyway via injeção de dependência.
     *
     * @param flyway Instância configurada do Flyway para executar migrações
     */
    public MigrationService(Flyway flyway) {
        this.flyway = flyway;
    }

    /**
     * Executa as migrações pendentes do banco de dados.
     *
     * Captura exceções específicas do Flyway e as converte em {@link MigrationException}
     * para abstrair a implementação do Flyway do restante da aplicação.
     *
     * @throws MigrationException se houver erro durante a execução das migrações
     */
    public void runMigrations() {
        try {
            logger.info("Executando migrações do banco de dados...");
            flyway.migrate();
            logger.info("Todas as migrações foram aplicadas com sucesso");
        } catch (FlywayException e) {
            logger.error("Falha ao executar migrations: {}", e.getMessage(), e);
            throw new MigrationException("Falha ao executar migrations", e);
        }
    }

    /**
     * Exceção customizada para erros durante a execução de migrações.
     *
     * Esta exceção é uma RuntimeException, permitindo que seja lançada sem
     * necessidade de declaração na assinatura do método. Encapsula erros
     * específicos do Flyway de forma agnóstica à implementação.
     */
    public static class MigrationException extends RuntimeException {
        /**
         * Construtor da exceção de migração.
         *
         * @param message Mensagem descritiva do erro
         * @param cause Exceção original que causou o erro
         */
        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}