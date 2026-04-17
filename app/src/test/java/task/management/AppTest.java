/*
 * Testes unitários da classe App.
 */
package task.management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    @Test
    @DisplayName("App deve ter uma saudação não nula")
    void appHasAGreeting() {
        String greeting = App.getGreeting();
        assertNotNull(greeting, "app should have a greeting");
    }

    @Test
    @DisplayName("A saudação deve mencionar Task Management")
    void appGreetingShouldMentionTaskManagement() {
        String greeting = App.getGreeting();
        assertTrue(greeting.contains("Task Management"),
                "Greeting should mention Task Management");
    }
}
