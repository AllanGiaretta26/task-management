/*
 * Task Management Application
 *
 * Application entry point with logging support.
 */
package task.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.management.infrastructure.jpa.JpaUtil;
import task.management.migrations.MigrationExecutor;
import task.management.ui.BoardMenu;

/**
 * Main application class for the Task Management system.
 *
 * Entry point for the application. Dispatches to migration execution
 * or the board menu based on CLI arguments, ensuring resources are
 * released cleanly even when errors occur.
 *
 * @author Allan Giaretta
 * @version 4.0
 */
public final class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    static final String GREETING = "Task Management Board System v3.1";

    private App() {
    }

    /**
     * Retorna a mensagem de saudação da aplicação.
     * Exposto para testes e para uso em telas introdutórias.
     *
     * @return mensagem de saudação
     */
    public static String getGreeting() {
        return GREETING;
    }

    /**
     * Main entry point for the Task Management application.
     *
     * @param args Command line arguments ("migrate" runs database migrations)
     */
    public static void main(String[] args) {
        logger.info(getGreeting());
        logger.info("Application started successfully");

        int exitCode = 0;
        try {
            if (args.length > 0 && "migrate".equals(args[0])) {
                logger.info("Running database migrations...");
                MigrationExecutor.run();
            } else {
                logger.info("Starting Task Management Board System...");
                runBoardMenu();
            }
        } catch (RuntimeException e) {
            logger.error("Falha na execução: {}", e.getMessage(), e);
            exitCode = 1;
        } finally {
            JpaUtil.close();
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Executes the board menu interactive interface.
     */
    private static void runBoardMenu() {
        logger.info("Launching board menu interface...");
        BoardMenu boardMenu = new BoardMenu();
        boardMenu.showMainMenu();
        logger.info("Board menu closed by user.");
    }
}
