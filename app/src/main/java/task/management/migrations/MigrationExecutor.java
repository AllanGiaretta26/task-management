package task.management.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor responsável por disparar a execução das migrações do banco de dados.
 *
 * Esta classe atua como um ponto de entrada que utiliza {@link MigrationRunnerFactory}
 * para criar e obter a instância de {@link MigrationService}, executando as migrações
 * e tratando erros que possam ocorrer durante o processo.
 *
 * @author Allan Giaretta
 * @version 2.0
 * @see MigrationService
 * @see MigrationRunnerFactory
 */
public class MigrationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);

    /**
     * Ponto de entrada para a execução das migrações.
     *
     * Obtém uma instância de {@link MigrationService} através da factory e executa
     * as migrações pendentes. Em caso de erro, registra a falha e encerra a aplicação.
     *
     * @param args Argumentos da linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        try {
            logger.info("Iniciando execução de migrações do banco de dados...");
            
            MigrationService migrationService = MigrationRunnerFactory.createMigrationService();
            migrationService.runMigrations();
            
            logger.info("Migrations executadas com sucesso!");
        } catch (Exception e) {
            logger.error("Erro ao executar migrations: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}