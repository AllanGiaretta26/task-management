package task.management.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade Task.
 */
class TaskTest {

    @Test
    @DisplayName("Deve criar uma tarefa com título e prioridade")
    void shouldCreateTaskWithTitleAndPriority() {
        Task task = new Task("Test Task", Task.Priority.HIGH);

        assertNotNull(task);
        assertEquals("Test Task", task.getTitle());
        assertEquals(Task.Priority.HIGH, task.getPriority());
        assertFalse(task.getCompleted());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve criar uma tarefa com título, descrição e prioridade")
    void shouldCreateTaskWithTitleDescriptionAndPriority() {
        Task task = new Task("Test Task", "Test Description", Task.Priority.MEDIUM);

        assertEquals("Test Task", task.getTitle());
        assertEquals("Test Description", task.getDescription());
        assertEquals(Task.Priority.MEDIUM, task.getPriority());
        assertFalse(task.getCompleted());
    }

    @Test
    @DisplayName("Deve usar prioridade MEDIUM quando prioridade é null")
    void shouldUseMediumPriorityWhenNull() {
        Task task = new Task("Test Task", null);

        assertEquals(Task.Priority.MEDIUM, task.getPriority());
    }

    @Test
    @DisplayName("Deve marcar tarefa como concluída")
    void shouldMarkTaskAsCompleted() {
        Task task = new Task("Test Task", Task.Priority.HIGH);
        assertFalse(task.getCompleted());

        task.complete();

        assertTrue(task.getCompleted());
    }

    @Test
    @DisplayName("Deve reabrir uma tarefa concluída")
    void shouldReopenCompletedTask() {
        Task task = new Task("Test Task", Task.Priority.HIGH);
        task.complete();
        assertTrue(task.getCompleted());

        task.reopen();

        assertFalse(task.getCompleted());
    }

    @Test
    @DisplayName("Deve atualizar o título da tarefa")
    void shouldUpdateTaskTitle() {
        Task task = new Task("Original Title", Task.Priority.LOW);
        
        task.updateTitle("New Title");

        assertEquals("New Title", task.getTitle());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar definir título vazio")
    void shouldThrowExceptionForEmptyTitle() {
        Task task = new Task("Test Task", Task.Priority.HIGH);

        assertThrows(IllegalArgumentException.class, () -> task.updateTitle(""));
        assertThrows(IllegalArgumentException.class, () -> task.updateTitle("   "));
        assertThrows(IllegalArgumentException.class, () -> task.updateTitle(null));
    }

    @Test
    @DisplayName("Deve atualizar a descrição da tarefa")
    void shouldUpdateTaskDescription() {
        Task task = new Task("Test Task", "Original Description", Task.Priority.HIGH);
        
        task.updateDescription("New Description");

        assertEquals("New Description", task.getDescription());
    }

    @Test
    @DisplayName("Deve atualizar a prioridade da tarefa")
    void shouldUpdateTaskPriority() {
        Task task = new Task("Test Task", Task.Priority.LOW);
        assertEquals(Task.Priority.LOW, task.getPriority());
        
        task.updatePriority(Task.Priority.URGENT);

        assertEquals(Task.Priority.URGENT, task.getPriority());
    }

    @Test
    @DisplayName("Deve usar prioridade MEDIUM ao definir prioridade como null")
    void shouldUseMediumPriorityWhenUpdatingToNull() {
        Task task = new Task("Test Task", Task.Priority.HIGH);
        
        task.updatePriority(null);

        assertEquals(Task.Priority.MEDIUM, task.getPriority());
    }

    @Test
    @DisplayName("Mesmo objeto deve ser igual a si mesmo")
    void sameObjectShouldBeEqualToItself() {
        Task task = new Task("Test Task", Task.Priority.HIGH);
        
        assertEquals(task, task);
    }

    @Test
    @DisplayName("Tarefa não deve ser igual a null")
    void taskShouldNotBeEqualToNull() {
        Task task = new Task("Test Task", Task.Priority.HIGH);
        
        assertNotEquals(null, task);
    }

    @Test
    @DisplayName("toString deve conter informações da tarefa")
    void toStringShouldContainTaskInfo() {
        Task task = new Task("Test Task", Task.Priority.HIGH);
        String toString = task.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("Test Task"));
        assertTrue(toString.contains("HIGH"));
    }

    @Test
    @DisplayName("Priority enum deve ter labels corretos")
    void priorityEnumShouldHaveCorrectLabels() {
        assertEquals("Baixa", Task.Priority.LOW.getLabel());
        assertEquals("Média", Task.Priority.MEDIUM.getLabel());
        assertEquals("Alta", Task.Priority.HIGH.getLabel());
        assertEquals("Urgente", Task.Priority.URGENT.getLabel());
    }
}
