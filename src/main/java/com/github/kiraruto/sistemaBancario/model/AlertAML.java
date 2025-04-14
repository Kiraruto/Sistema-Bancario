package com.github.kiraruto.sistemaBancario.model;

import com.github.kiraruto.sistemaBancario.model.enums.EnumStatusAlert;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;

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

    public AlertAML(Transaction transaction, String cpf) {
        this.accountId = transaction.getId().toString();
        this.amount = transaction.getAmount();
        this.date = LocalDateTime.now();
        this.cpf = cpf;
        this.status = EnumStatusAlert.EM_ANALISE;
        this.observacoes = "Transição pendente da conta" + transaction.getId();
    }
}
