package task.management.migrations;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.management.infrastructure.config.DatabaseConfig;
import task.management.infrastructure.config.DatabaseConfigLoader;

/**
 * Factory para criação de instâncias de {@link MigrationService}.
 *
 * Implementa o padrão Factory Method, centralizando a lógica de configuração
 * e instanciação do serviço de migrações. As configurações do banco de dados
 * são carregadas pelo {@link DatabaseConfigLoader}.
 *
 * @author Allan Giaretta
 * @version 4.0
 */
public class MigrationRunnerFactory {

    private static final Logger logger = LoggerFactory.getLogger(MigrationRunnerFactory.class);

    private MigrationRunnerFactory() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada");
    }

    /**
     * Cria e retorna uma instância configurada de {@link MigrationService}.
     *
     * @return Uma nova instância de MigrationService configurada e pronta para uso
     */
    public static MigrationService createMigrationService() {
        DatabaseConfig config = DatabaseConfigLoader.load();

        logger.info("Configurando Flyway com URL: {}", config.url());

        Flyway flyway = Flyway.configure()
                .dataSource(config.url(), config.username(), config.password())
                .load();

        return new MigrationService(flyway);
    }
}
