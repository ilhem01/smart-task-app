export interface Task {
  id?: number;
  title: string;
  description?: string;
  summary?: string;
  priority?: string;
  deadline?: number;
  effort?: number;
  /** low | medium | high */
  stressLevel?: string;
  dueDate?: string;
  completed?: boolean;
}
