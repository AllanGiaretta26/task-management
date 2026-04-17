package task.management.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base para testes de integração que exercitam JPA contra o H2 in-memory
 * configurado na persistence-unit "test-pu".
 *
 * Cada classe de teste compartilha um único {@link EntityManagerFactory}
 * criado em {@link BeforeAll} e encerrado em {@link AfterAll}. Antes de cada
 * teste, todas as tabelas são limpas respeitando a ordem de FKs.
 */
public abstract class AbstractJpaIT {

    private static final String TEST_PU = "test-pu";

    protected static EntityManagerFactory emf;

    @BeforeAll
    static void initEmf() {
        emf = Persistence.createEntityManagerFactory(TEST_PU);
    }

    @AfterAll
    static void closeEmf() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    void cleanDatabase() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM ColumnHistory").executeUpdate();
            em.createQuery("DELETE FROM Blockade").executeUpdate();
            em.createQuery("DELETE FROM Card").executeUpdate();
            em.createQuery("DELETE FROM BoardColumn").executeUpdate();
            em.createQuery("DELETE FROM Board").executeUpdate();
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    protected EntityManager newEntityManager() {
        return emf.createEntityManager();
    }
}
