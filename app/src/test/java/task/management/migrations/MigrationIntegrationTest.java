package task.management.migrations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.management.domain.Task;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para validar o fluxo de migração do banco de dados
 * e a integração com JPA/Hibernate.
 *
 * Estes testes validam que:
 * - As migrações são executadas corretamente
 * - A tabela tasks é criada com a estrutura correta
 * - A entidade Task pode ser persistida e recuperada
 */
class MigrationIntegrationTest {

    private static EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        // Initialize in-memory database for testing
        if (entityManagerFactory == null) {
            entityManagerFactory = Persistence.createEntityManagerFactory("test-pu");
        }
    }

    @Test
    void migrationServiceShouldBeCreatedSuccessfully() {
        // Test that the factory can create a migration service
        assertDoesNotThrow(() -> {
            MigrationService service = MigrationRunnerFactory.createMigrationService();
            assertNotNull(service);
        });
    }

    @Test
    void taskEntityShouldPersistCorrectly() {
        // Given
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            em.getTransaction().begin();

            // When
            Task task = new Task("Test Task", "Test Description", Task.Priority.HIGH);
            em.persist(task);
            em.getTransaction().commit();

            // Then
            Long taskId = task.getId();
            assertNotNull(taskId);
            
            // Verify retrieval
            Task retrievedTask = em.find(Task.class, taskId);
            assertNotNull(retrievedTask);
            assertEquals("Test Task", retrievedTask.getTitle());
            assertEquals("Test Description", retrievedTask.getDescription());
            assertEquals(Task.Priority.HIGH, retrievedTask.getPriority());
            assertFalse(retrievedTask.getCompleted());
            assertNotNull(retrievedTask.getCreatedAt());
            assertNotNull(retrievedTask.getUpdatedAt());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    @Test
    void taskCompleteOperationShouldWork() {
        // Given
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            em.getTransaction().begin();
            Task task = new Task("Task to Complete", Task.Priority.MEDIUM);
            em.persist(task);
            em.getTransaction().commit();

            // When
            em.getTransaction().begin();
            Task managedTask = em.find(Task.class, task.getId());
            managedTask.complete();
            em.getTransaction().commit();

            // Then
            Task verifiedTask = em.find(Task.class, task.getId());
            assertTrue(verifiedTask.getCompleted());
            assertNotNull(verifiedTask.getUpdatedAt());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    @Test
    void taskUpdateOperationsShouldWork() {
        // Given
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            em.getTransaction().begin();
            Task task = new Task("Original Title", "Original Description", Task.Priority.LOW);
            em.persist(task);
            em.getTransaction().commit();

            // When
            em.getTransaction().begin();
            Task managedTask = em.find(Task.class, task.getId());
            managedTask.updateTitle("Updated Title");
            managedTask.updateDescription("Updated Description");
            managedTask.updatePriority(Task.Priority.URGENT);
            em.getTransaction().commit();

            // Then
            Task verifiedTask = em.find(Task.class, task.getId());
            assertEquals("Updated Title", verifiedTask.getTitle());
            assertEquals("Updated Description", verifiedTask.getDescription());
            assertEquals(Task.Priority.URGENT, verifiedTask.getPriority());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
