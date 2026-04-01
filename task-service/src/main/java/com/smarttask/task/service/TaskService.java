package com.smarttask.task.service;

import com.smarttask.task.entity.Task;
import com.smarttask.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final String FALLBACK_PRIORITY = "MEDIUM";
    private static final int FALLBACK_DEADLINE = 0;
    private static final int FALLBACK_EFFORT = 3;

    private final TaskRepository taskRepository;
    private final AiService aiService;

    public Task createTask(Task task) {
        Map<String, Object> aiResult = aiService.analyzeTask(task.getTitle());
        task.setSummary(asString(aiResult.get("summary")));

        String aiPriority = asString(aiResult.get("priority"));
        if (isBlank(task.getPriority())) {
            task.setPriority(isBlank(aiPriority) ? FALLBACK_PRIORITY : aiPriority);
        }

        Integer aiDeadline = asInteger(aiResult.get("deadline"));
        task.setDeadline(aiDeadline == null ? FALLBACK_DEADLINE : aiDeadline);

        Integer aiEffort = asInteger(aiResult.get("effort"));
        task.setEffort(clampEffort(aiEffort == null ? FALLBACK_EFFORT : aiEffort));

        String stress = asString(aiResult.get("stressLevel"));
        if (!isBlank(stress)) {
            task.setStressLevel(stress.trim().toLowerCase());
        }

        return taskRepository.save(task);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                return null;
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int clampEffort(int effort) {
        if (effort < 1) {
            return 1;
        }
        if (effort > 5) {
            return 5;
        }
        return effort;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAllByOrderByDueDateAscIdAsc();
    }

    public Map<String, Object> getRecommendation() {
        List<Task> tasks = taskRepository.findAllByOrderByDueDateAscIdAsc();
        return aiService.recommendTasks(tasks);
    }

    public TaskListWithRecommendation getAllTasksWithRecommendation() {
        List<Task> tasks = taskRepository.findAllByOrderByDueDateAscIdAsc();
        Map<String, Object> recommendation = aiService.recommendTasks(tasks);
        return new TaskListWithRecommendation(tasks, recommendation);
    }

    @Transactional
    public Optional<Task> updateTask(Long id, Task task) {
        return taskRepository.findById(id).map(existing -> {
            existing.setTitle(task.getTitle());
            existing.setDescription(task.getDescription());
            existing.setPriority(task.getPriority());
            existing.setDueDate(task.getDueDate());
            existing.setCompleted(task.isCompleted());
            return taskRepository.save(existing);
        });
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public List<Task> findAll() {
        return taskRepository.findAllByOrderByDueDateAscIdAsc();
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    @Transactional
    public Task create(Task task) {
        return taskRepository.save(task);
    }

    @Transactional
    public Optional<Task> update(Long id, Task updates) {
        return taskRepository.findById(id).map(existing -> {
            existing.setTitle(updates.getTitle());
            existing.setDescription(updates.getDescription());
            existing.setPriority(updates.getPriority());
            existing.setDueDate(updates.getDueDate());
            existing.setCompleted(updates.isCompleted());
            return taskRepository.save(existing);
        });
    }

    @Transactional
    public void deleteById(Long id) {
        taskRepository.deleteById(id);
    }

    public record TaskListWithRecommendation(List<Task> tasks, Map<String, Object> recommendation) {
    }
}
