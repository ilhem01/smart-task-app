package com.smarttask.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String summary;

    @Column
    private String priority;

    @Column
    private Integer deadline;

    @Column
    private Integer effort; // 1-5

    @Column
    private Double aiScore; // calculated later

    /** low | medium | high — from keyword + AI analysis */
    @Column(length = 20)
    private String stressLevel;

    @Column
    private LocalDate dueDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean completed = false;
}
