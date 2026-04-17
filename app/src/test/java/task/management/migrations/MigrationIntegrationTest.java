package task.management.migrations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testes de integração para validar o fluxo de migração do banco de dados.
 */
class MigrationIntegrationTest {

    @Test
    void migrationServiceShouldBeCreatedSuccessfully() {
        assertDoesNotThrow(() -> {
            MigrationService service = MigrationRunnerFactory.createMigrationService();
            assertNotNull(service);
        });
    }
}
