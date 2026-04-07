package task.management.migrations;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Factory para criação de instâncias de {@link MigrationService}.
 *
 * Implementa o padrão Factory Method, centralizando a lógica de configuração
 * e instanciação do serviço de migrações. Isso facilita testes unitários e
 * permite alterar a estratégia de criação em um único local.
 *
 * As configurações do banco de dados são carregadas exclusivamente do arquivo
 * {@code database.properties} presente no classpath.
 *
 * @author Allan Giaretta
 * @version 3.0
 */
public class MigrationRunnerFactory {

    private static final Logger logger = LoggerFactory.getLogger(MigrationRunnerFactory.class);

    private static final String DB_CONFIG_FILE = "database.properties";

    /**
     * Cria e retorna uma instância configurada de {@link MigrationService}.
     *
     * As credenciais são carregadas exclusivamente do arquivo database.properties.
     *
     * @return Uma nova instância de MigrationService configurada e pronta para uso
     * @throws RuntimeException se não for possível carregar as configurações do banco
     */
    public static MigrationService createMigrationService() {
        DatabaseConfig config = loadDatabaseConfig();

        logger.info("Configurando Flyway com URL: {}", config.url);

        Flyway flyway = Flyway.configure()
                .dataSource(config.url, config.username, config.password)
                .load();

        return new MigrationService(flyway);
    }

    /**
     * Carrega configurações do banco de dados do arquivo database.properties.
     *
     * @return DatabaseConfig com as credenciais carregadas
     * @throws RuntimeException se o arquivo não for encontrado ou as propriedades estiverem ausentes
     */
    private static DatabaseConfig loadDatabaseConfig() {
        try {
            Properties properties = loadPropertiesFile();
            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");

            if (url == null || username == null || password == null) {
                throw new RuntimeException(
                        "Propriedades obrigatórias ausentes em database.properties: db.url, db.username, db.password");
            }

            logger.info("Usando configurações de database.properties");
            return new DatabaseConfig(url, username, password);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível carregar database.properties: " + e.getMessage(), e);
        }
    }

    /**
     * Carrega o arquivo database.properties do classpath.
     *
     * @return Properties carregadas do arquivo
     * @throws IOException se houver erro na leitura
     */
    private static Properties loadPropertiesFile() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = MigrationRunnerFactory.class.getClassLoader()
                .getResourceAsStream(DB_CONFIG_FILE)) {

            if (input == null) {
                throw new IOException("Arquivo " + DB_CONFIG_FILE + " não encontrado no classpath");
            }

            properties.load(input);
        }
        return properties;
    }

    /**
     * Classe interna para armazenar configurações do banco de dados.
     */
    private static class DatabaseConfig {
        final String url;
        final String username;
        final String password;

        DatabaseConfig(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }
}