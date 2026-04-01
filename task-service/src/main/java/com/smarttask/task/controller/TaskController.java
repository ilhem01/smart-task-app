package com.smarttask.task.controller;

import com.smarttask.task.entity.Task;
import com.smarttask.task.service.AiService;
import com.smarttask.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task CRUD — authentication is enforced at the API Gateway when using port 8080")
public class TaskController {

    private static final String BEARER = "bearerAuth";

    private final TaskService taskService;
    private final AiService aiService;

    @GetMapping
    @Operation(summary = "List all tasks")
    @SecurityRequirement(name = BEARER)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = Task.class)))
    })
    public ResponseEntity<List<Task>> getAll() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/recommendation")
    @Operation(summary = "Get recommendation for all tasks")
    @SecurityRequirement(name = BEARER)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendation JSON returned")
    })
    public ResponseEntity<Map<String, Object>> getRecommendation() {
        return ResponseEntity.ok(taskService.getRecommendation());
    }

    @PostMapping
    @Operation(summary = "Create a task")
    @SecurityRequirement(name = BEARER)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(schema = @Schema(implementation = Task.class)))
    })
    public ResponseEntity<Task> create(@RequestBody Task task) {
        Task created = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task", description = "Replace fields including title, description, priority, dueDate, and completed.")
    @SecurityRequirement(name = BEARER)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(schema = @Schema(implementation = Task.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Task> update(@PathVariable Long id, @RequestBody Task task) {
        return taskService.updateTask(id, task)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    @SecurityRequirement(name = BEARER)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (taskService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ai/analyze")
    @Operation(summary = "Analyze task title with AI")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI result with summary, priority, and deadline"),
            @ApiResponse(responseCode = "500", description = "Webhook call failed")
    })
    public ResponseEntity<Map<String, Object>> analyzeTitle(@RequestBody AiTitleRequest request) {
        try {
            Map<String, Object> result = aiService.analyzeTitle(request.title());
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/ai/recommend")
    @Operation(summary = "Get AI task recommendations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendation JSON returned"),
            @ApiResponse(responseCode = "500", description = "Webhook call failed")
    })
    public ResponseEntity<Map<String, Object>> recommendTasks(@RequestBody List<Task> tasks) {
        try {
            Map<String, Object> result = aiService.recommendTasks(tasks);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public record AiTitleRequest(String title) {
    }
}
