package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatusAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertAMLRepository extends JpaRepository<AlertAML, UUID> {
    List<AlertAML> findByStatus(EnumStatusAlert enumStatusAlert);
}