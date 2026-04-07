package task.management.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utilitário para gerenciar o EntityManager da aplicação.
 *
 * As configurações do banco de dados são carregadas exclusivamente do arquivo
 * {@code database.properties} presente no classpath.
 *
 * @author Allan Giaretta
 * @version 2.0
 */
public final class JpaUtil {

    private static final Logger logger = LoggerFactory.getLogger(JpaUtil.class);

    private static final String PERSISTENCE_UNIT = "task-management-pu";
    private static final String DB_CONFIG_FILE = "database.properties";

    private static EntityManagerFactory entityManagerFactory;

    /**
     * Construtor privado para impedir instanciação.
     */
    private JpaUtil() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada");
    }

    /**
     * Obtém o EntityManager singleton.
     *
     * @return EntityManager configurado
     */
    public static EntityManager getEntityManager() {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            entityManagerFactory = createEntityManagerFactory();
        }
        return entityManagerFactory.createEntityManager();
    }

    /**
     * Fecha o EntityManagerFactory.
     * Deve ser chamado no encerramento da aplicação.
     */
    public static void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    /**
     * Cria o EntityManagerFactory com as configurações de banco de dados.
     *
     * @return EntityManagerFactory configurado
     */
    private static EntityManagerFactory createEntityManagerFactory() {
        DatabaseConfig config = loadDatabaseConfig();

        logger.info("Configurando JPA com URL: {}", config.url);

        Map<String, String> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", config.url);
        properties.put("jakarta.persistence.jdbc.user", config.username);
        properties.put("jakarta.persistence.jdbc.password", config.password);

        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
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
        try (InputStream input = JpaUtil.class.getClassLoader()
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
