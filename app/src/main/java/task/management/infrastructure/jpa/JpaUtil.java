package task.management.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.management.infrastructure.config.DatabaseConfig;
import task.management.infrastructure.config.DatabaseConfigLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para gerenciar o EntityManager da aplicação.
 *
 * As configurações do banco de dados são carregadas pelo
 * {@link DatabaseConfigLoader} a partir do arquivo {@code database.properties}.
 *
 * @author Allan Giaretta
 * @version 4.0
 */
public final class JpaUtil {

    private static final Logger logger = LoggerFactory.getLogger(JpaUtil.class);

    private static final String PERSISTENCE_UNIT = "task-management-pu";

    private static EntityManagerFactory entityManagerFactory;

    private JpaUtil() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada");
    }

    /**
     * Obtém o EntityManager singleton.
     *
     * @return EntityManager configurado
     */
    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /**
     * Obtém o EntityManagerFactory singleton.
     *
     * Services devem receber este factory e abrir/fechar um {@link EntityManager}
     * por operação via try-with-resources, mantendo o ciclo de vida curto e
     * evitando estado compartilhado entre chamadas.
     *
     * @return EntityManagerFactory configurado
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            entityManagerFactory = createEntityManagerFactory();
        }
        return entityManagerFactory;
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

    private static EntityManagerFactory createEntityManagerFactory() {
        DatabaseConfig config = DatabaseConfigLoader.load();

        logger.info("Configurando JPA com URL: {}", config.url());

        Map<String, String> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", config.url());
        properties.put("jakarta.persistence.jdbc.user", config.username());
        properties.put("jakarta.persistence.jdbc.password", config.password());

        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
    }
}
