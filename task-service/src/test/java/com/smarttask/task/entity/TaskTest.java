package com.smarttask.task.entity;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaskTest {

    @Test
    void builderCreatesTask() {
        Task task = Task.builder()
                .title("Example")
                .description("Desc")
                .priority("HIGH")
                .dueDate(LocalDate.of(2026, 1, 15))
                .completed(false)
                .build();

        assertEquals("Example", task.getTitle());
        assertEquals("Desc", task.getDescription());
        assertEquals("HIGH", task.getPriority());
        assertEquals(LocalDate.of(2026, 1, 15), task.getDueDate());
        assertFalse(task.isCompleted());
    }
}
