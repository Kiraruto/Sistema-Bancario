package com.github.kiraruto.sistemaBancario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scheduled_transfer")
public class ScheduledTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID sourceAccount;

    private UUID targetAccount;

    private BigDecimal amount;

    private LocalDateTime scheduledDate;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    private int retryCount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
