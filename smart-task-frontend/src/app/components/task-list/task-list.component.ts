import { Component, OnDestroy, OnInit } from '@angular/core';

import { Task } from '../../models/task.model';
import { AiService } from '../../services/ai.service';
import { TaskService } from '../../services/task.service';

@Component({
  selector: 'app-task-list',
  templateUrl: './task-list.component.html',
  styleUrls: ['./task-list.component.scss']
})
export class TaskListComponent implements OnInit, OnDestroy {
  tasks: Task[] = [];
  recommendation: Record<string, unknown> | null = null;
  recommendedTaskId: number | null = null;
  reason = '';
  stressLevel = '';
  loading = false;
  recommendedTask: Task | null = null;
  title = '';
  description = '';
  priority = 'MEDIUM';
  dueDate = '';
  errorMessage = '';

  loadingTasks = false;
  loadingRecommendation = false;
  savingTask = false;
  deletingId: number | null = null;
  updatingId: number | null = null;
  private autoRefreshTimer: ReturnType<typeof setInterval> | null = null;

  constructor(
    private readonly taskService: TaskService,
    private readonly aiService: AiService
  ) {}

  ngOnInit(): void {
    this.loadTasks();
    this.loadRecommendation();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  loadTasks(): void {
    this.errorMessage = '';
    this.loadingTasks = true;
    this.taskService.getTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks ?? [];
        this.recommendation = null;
        this.recommendedTaskId = null;
        this.recommendedTask = null;
        this.reason = '';
        this.stressLevel = '';
        this.loadingTasks = false;

      },
      error: (error: Error) => {
        this.loadingTasks = false;
        this.loadingRecommendation = false;
        this.loading = false;
        this.errorMessage = error.message || 'Failed to load tasks.';
      }
    });
  }

  loadRecommendation(): void {
    this.loadingRecommendation = true;
    this.loading = true;
    console.log('Calling AI recommendation API...');
    this.aiService.getRecommendation().subscribe({
      next: (recommendation) => {
        this.recommendation = recommendation;
        this.recommendedTaskId = this.getRecommendedTaskId(this.recommendation);
        this.recommendedTask = this.resolveRecommendedTask(this.tasks, this.recommendedTaskId, this.recommendation);
        this.reason = this.getRecommendationReason(this.recommendation);
        this.stressLevel = this.getRecommendationStressLevel(this.recommendation);
        this.errorMessage = '';
        this.loadingRecommendation = false;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load AI recommendation.';
        this.loadingRecommendation = false;
        this.loading = false;
      }
    });
  }

  onGetAiRecommendation(): void {
    this.loadRecommendation();
  }

  onCreateTask(): void {
    this.errorMessage = '';

    if (!this.title.trim()) {
      this.errorMessage = 'Task title is required.';
      return;
    }

    const newTask: Task = {
      title: this.title.trim(),
      description: this.description.trim() || undefined,
      priority: this.priority,
      dueDate: this.dueDate || undefined,
      completed: false
    };

    this.savingTask = true;
    this.taskService.createTask(newTask).subscribe({
      next: () => {
        this.title = '';
        this.description = '';
        this.priority = 'MEDIUM';
        this.dueDate = '';
        this.savingTask = false;
        this.loadTasks();
        this.loadRecommendation();
      },
      error: (error: Error) => {
        this.savingTask = false;
        this.errorMessage = error.message || 'Failed to create task.';
      }
    });
  }

  onToggleCompleted(task: Task, event: Event): void {
    const id = task.id;
    if (id == null) {
      return;
    }

    const checked = (event.target as HTMLInputElement).checked;
    this.errorMessage = '';

    const updated: Task = {
      ...task,
      completed: checked
    };

    this.updatingId = id;
    this.taskService.updateTask(id, updated).subscribe({
      next: (saved) => {
        const idx = this.tasks.findIndex((t) => t.id === id);
        if (idx >= 0) {
          this.tasks[idx] = saved;
        }
        this.updatingId = null;
      },
      error: (error: Error) => {
        this.updatingId = null;
        this.errorMessage = error.message || 'Failed to update task.';
        this.loadTasks();
        this.loadRecommendation();
      }
    });
  }

  trackByTaskId(_index: number, task: Task): number | undefined {
    return task.id;
  }

  onDeleteTask(id?: number): void {
    if (id == null) {
      return;
    }

    this.errorMessage = '';
    this.deletingId = id;
    this.taskService.deleteTask(id).subscribe({
      next: () => {
        this.tasks = this.tasks.filter((task) => task.id !== id);
        this.deletingId = null;
      },
      error: (error: Error) => {
        this.deletingId = null;
        this.errorMessage = error.message || 'Failed to delete task.';
      }
    });
  }

  isRecommendedTask(task: Task): boolean {
    return this.recommendedTaskId != null && task.id != null && this.recommendedTaskId === task.id;
  }

  isUrgentTask(task: Task): boolean {
    const highPriority = (task.priority ?? '').toUpperCase() === 'HIGH';
    const shortDeadline = typeof task.deadline === 'number' && task.deadline > 0 && task.deadline <= 1;
    return !task.completed && (highPriority || shortDeadline);
  }

  private getRecommendedTaskId(recommendation: Record<string, unknown> | null): number | null {
    if (!recommendation) {
      return null;
    }
    const base = this.getRecommendationObject(recommendation);
    return (
      this.asNumber(base['recommendedTaskId']) ??
      this.asNumber(base['recommended_task_id']) ??
      this.asNumber(base['taskId']) ??
      this.asNumber(base['task_id']) ??
      this.asNumber(base['id']) ??
      this.asNumber((base['recommendedTask'] as Record<string, unknown> | undefined)?.['id'])
    );
  }

  private resolveRecommendedTask(
    tasks: Task[],
    recommendedTaskId: number | null,
    recommendation: Record<string, unknown> | null
  ): Task | null {
    if (recommendedTaskId == null) {
      const title = this.getRecommendedTaskTitle(recommendation);
      if (!title) {
        return null;
      }
      const normalized = title.toLowerCase();
      return tasks.find((t) => t.title.toLowerCase() === normalized) ?? null;
    }
    return tasks.find((t) => t.id === recommendedTaskId) ?? null;
  }

  private getRecommendationReason(recommendation: Record<string, unknown> | null): string {
    if (!recommendation) {
      return '';
    }
    const base = this.getRecommendationObject(recommendation);
    return (
      this.asString(base['reason']) ||
      this.asString(base['why']) ||
      this.asString(base['explanation']) ||
      ''
    );
  }

  private getRecommendationStressLevel(recommendation: Record<string, unknown> | null): string {
    if (!recommendation) {
      return '';
    }
    const base = this.getRecommendationObject(recommendation);
    return (
      this.asString(base['stressLevel']) ||
      this.asString(base['stress_level']) ||
      this.asString(base['stress']) ||
      ''
    );
  }

  private getRecommendedTaskTitle(recommendation: Record<string, unknown> | null): string {
    if (!recommendation) {
      return '';
    }
    const base = this.getRecommendationObject(recommendation);
    return (
      this.asString(base['recommendedTaskTitle']) ||
      this.asString(base['recommended_task_title']) ||
      this.asString(base['taskTitle']) ||
      this.asString(base['title']) ||
      this.asString((base['recommendedTask'] as Record<string, unknown> | undefined)?.['title']) ||
      ''
    );
  }

  private getRecommendationObject(recommendation: Record<string, unknown>): Record<string, unknown> {
    const nested = recommendation['recommendation'];
    if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
      return nested as Record<string, unknown>;
    }
    return recommendation;
  }

  private asString(value: unknown): string {
    return typeof value === 'string' ? value.trim() : '';
  }

  private asNumber(value: unknown): number | null {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === 'string' && value.trim()) {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    }
    return null;
  }

  private stopAutoRefresh(): void {
    if (this.autoRefreshTimer) {
      clearInterval(this.autoRefreshTimer);
      this.autoRefreshTimer = null;
    }
  }

  private startAutoRefresh(): void {
    this.stopAutoRefresh();
    this.autoRefreshTimer = setInterval(() => {
      this.loadTasks();
      this.loadRecommendation();
    }, 10000);
  }
}
