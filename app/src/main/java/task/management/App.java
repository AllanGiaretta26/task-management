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
 * This class serves as the entry point for the application and provides
 * logging capabilities using SLF4J/Logback.
 *
 * @author Allan Giaretta
 * @version 3.0
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * Returns a greeting message for the application.
     *
     * @return Greeting message
     */
    public String getGreeting() {
        return "Task Management Board System v3.0";
    }

    /**
     * Main entry point for the Task Management application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        App app = new App();
        logger.info(app.getGreeting());
        logger.info("Application started successfully");

        if (args.length > 0 && "migrate".equals(args[0])) {
            logger.info("Running database migrations...");
            MigrationExecutor.main(args);
        } else {
            logger.info("Starting Task Management Board System...");
            try {
                runBoardMenu();
            } finally {
                JpaUtil.close();
            }
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

