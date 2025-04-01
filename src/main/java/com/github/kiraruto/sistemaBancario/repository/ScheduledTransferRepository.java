package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, UUID> {

    List<ScheduledTransfer> findByStatusAndScheduledDateBefore(EnumStatus enumStatus, LocalDateTime now);

    List<ScheduledTransfer> findAllByUser(User user);

    List<ScheduledTransfer> findByStatus(EnumStatus enumStatus);

    ScheduledTransfer findStatusByid(UUID id);
}
