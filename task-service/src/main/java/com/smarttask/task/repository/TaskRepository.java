package com.smarttask.task.repository;

import com.smarttask.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Due date ascending ({@code NULL} last), then priority HIGH → MEDIUM → LOW (case-insensitive),
     * then id ascending. Unknown or blank priority sorts after LOW.
     */
    @Query("SELECT t FROM Task t ORDER BY t.dueDate ASC NULLS LAST, "
            + "CASE UPPER(COALESCE(t.priority, '')) "
            + "WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END ASC, "
            + "t.id ASC")
    List<Task> findAllByOrderByDueDateAscIdAsc();
}
