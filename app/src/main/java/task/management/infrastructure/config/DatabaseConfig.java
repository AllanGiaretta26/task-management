package task.management.infrastructure.config;

/**
 * Configurações de conexão com o banco de dados.
 *
 * Record imutável carregado uma vez pelo {@link DatabaseConfigLoader} e
 * reutilizado por JpaUtil e MigrationRunnerFactory.
 *
 * @param url      URL JDBC
 * @param username Usuário
 * @param password Senha
 *
 * @author Allan Giaretta
 * @version 1.0
 */
public record DatabaseConfig(String url, String username, String password) {
}
