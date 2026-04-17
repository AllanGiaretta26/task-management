package task.management.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor responsável por disparar a execução das migrações do banco de dados.
 *
 * Utiliza {@link MigrationRunnerFactory} para criar a instância de
 * {@link MigrationService} e executar as migrações pendentes.
 *
 * <p>A classe deixa que o chamador decida como lidar com falhas (propaga a
 * exceção em vez de chamar {@code System.exit}), permitindo que recursos
 * como o {@code EntityManagerFactory} sejam corretamente fechados.
 *
 * @author Allan Giaretta
 * @version 4.0
 */
public final class MigrationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);

    private MigrationExecutor() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada");
    }

    /**
     * Executa as migrações pendentes.
     *
     * @throws MigrationService.MigrationException se houver falha na execução
     */
    public static void run() {
        logger.info("Iniciando execução de migrações do banco de dados...");
        MigrationService migrationService = MigrationRunnerFactory.createMigrationService();
        migrationService.runMigrations();
        logger.info("Migrations executadas com sucesso!");
    }

    /**
     * Ponto de entrada standalone para execução via linha de comando.
     *
     * Chamado quando a aplicação é iniciada com argumento "migrate".
     *
     * @param args Argumentos da linha de comando (ignorados)
     */
    public static void main(String[] args) {
        try {
            run();
        } catch (RuntimeException e) {
            logger.error("Erro ao executar migrations: {}", e.getMessage(), e);
            // Propaga para o chamador (App) fazer cleanup antes de encerrar
            throw e;
        }
    }
}
