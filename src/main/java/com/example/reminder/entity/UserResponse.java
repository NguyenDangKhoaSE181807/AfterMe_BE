package com.example.reminder.entity;

import com.example.reminder.domain.enums.UserResponseAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_responses")
public class UserResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_instance_id", nullable = false)
    private ReminderInstance reminderInstance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserResponseAction action;

    @Column(name = "response_time", nullable = false)
    private LocalDateTime responseTime;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}





