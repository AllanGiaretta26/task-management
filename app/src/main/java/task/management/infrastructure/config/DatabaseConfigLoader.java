package task.management.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Carregador centralizado das configurações do banco de dados.
 *
 * Lê o arquivo {@code database.properties} do classpath e valida a presença
 * das propriedades obrigatórias. Antes desta classe, a lógica de carregamento
 * estava duplicada em {@code JpaUtil} e {@code MigrationRunnerFactory}.
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public final class DatabaseConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigLoader.class);

    private static final String DB_CONFIG_FILE = "database.properties";
    private static final String PROP_URL = "db.url";
    private static final String PROP_USERNAME = "db.username";
    private static final String PROP_PASSWORD = "db.password";

    private DatabaseConfigLoader() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada");
    }

    /**
     * Carrega a configuração do banco de dados do arquivo {@code database.properties}.
     *
     * @return Configuração de banco validada
     * @throws DatabaseConfigException se o arquivo não existir, for ilegível ou
     *                                 se alguma propriedade obrigatória estiver ausente
     */
    public static DatabaseConfig load() {
        Properties properties = loadPropertiesFile();

        String url = properties.getProperty(PROP_URL);
        String username = properties.getProperty(PROP_USERNAME);
        String password = properties.getProperty(PROP_PASSWORD);

        if (isBlank(url) || isBlank(username) || password == null) {
            throw new DatabaseConfigException(
                    "Propriedades obrigatórias ausentes em " + DB_CONFIG_FILE
                            + ": " + PROP_URL + ", " + PROP_USERNAME + ", " + PROP_PASSWORD);
        }

        logger.info("Configurações de banco carregadas de {}", DB_CONFIG_FILE);
        return new DatabaseConfig(url, username, password);
    }

    private static Properties loadPropertiesFile() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfigLoader.class.getClassLoader()
                .getResourceAsStream(DB_CONFIG_FILE)) {

            if (input == null) {
                throw new DatabaseConfigException(
                        "Arquivo " + DB_CONFIG_FILE + " não encontrado no classpath");
            }

            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new DatabaseConfigException(
                    "Não foi possível ler " + DB_CONFIG_FILE + ": " + e.getMessage(), e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Exceção lançada quando a configuração de banco não pode ser carregada.
     */
    public static class DatabaseConfigException extends RuntimeException {
        public DatabaseConfigException(String message) {
            super(message);
        }

        public DatabaseConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
