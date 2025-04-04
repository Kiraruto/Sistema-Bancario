package com.github.kiraruto.sistemaBancario.model;

import com.github.kiraruto.sistemaBancario.model.enums.EnumStatusAlert;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alert_aml")
public class AlertAML {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private String cpf;

    @Enumerated(EnumType.STRING)
    private EnumStatusAlert status;

    private String observacoes;
}
