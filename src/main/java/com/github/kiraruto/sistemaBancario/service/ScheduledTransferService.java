package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.ScheduledTransferDTO;
import com.github.kiraruto.sistemaBancario.mapper.ScheduledTransferMapper;
import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.repository.ScheduledTransferRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.ValidateScheduleTransfer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledTransferService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTransferService.class);
    private final ValidateScheduleTransfer scheduleTransferValidate;

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final ScheduledTransferExecutor scheduledTransferExecutor;
    private final UserRepository userRepository;

    public List<ScheduledTransfer> getScheduled() {
        return scheduledTransferRepository.findAll();
    }

    public ScheduledTransfer createScheduledTransfer(ScheduledTransferDTO dto) {
        ScheduledTransfer scheduledTransfer = new ScheduledTransfer();
        scheduledTransfer.setSourceAccount(dto.accountSends());
        scheduledTransfer.setTargetAccount(dto.accountReceive());
        scheduledTransfer.setAmount(dto.amount());
        scheduledTransfer.setScheduledDate(dto.scheduledDate());
        scheduledTransfer.setStatus(EnumStatus.PENDENTE);
        scheduledTransfer.setUser(new User(UUID.fromString(dto.userId())));

        return scheduledTransferRepository.save(scheduledTransfer);
    }


    public List<ScheduledTransfer> processScheduledTransfers() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTransfer> transfers = scheduledTransferRepository.findByStatusAndScheduledDateBefore(EnumStatus.PENDENTE, now);
        List<ScheduledTransfer> transferList = new ArrayList<>();

        for (ScheduledTransfer transfer : transfers) {
            try {
                transferList.add(scheduledTransferExecutor.execute(transfer));
            } catch (Exception e) {
                logger.error("Falha ao executar transferência agendada {}: {}", transfer.getId(), e.getMessage());
                transfer.setStatus(EnumStatus.FALHOU);
                transfer.setRetryCount(transfer.getRetryCount() + 1);
                scheduledTransferRepository.save(transfer);
                transferList.add(transfer);
            }
        }
        return transferList;
    }


    @Scheduled(fixedRate = 300000)
    public void checkAndProcessTransfers() {
        logger.info("Verificando transferências agendadas...");
        List<ScheduledTransfer> processedTransfers = processScheduledTransfers();
        logger.info("Transferências processadas: " + processedTransfers.size());
    }


    public ScheduledTransfer getScheduledById(UUID uuid) {
        return scheduledTransferRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A transferência agendada com o ID fornecido não foi encontrada."));
    }

    public ScheduledTransfer updateScheduledTransfer(UUID uuid, @Valid ScheduledTransferDTO scheduledTransferDTO) {
        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A transferência agendada com o ID fornecido não foi encontrada."));

        scheduledTransfer.setSourceAccount(scheduledTransferDTO.accountSends());
        scheduledTransfer.setTargetAccount(scheduledTransferDTO.accountReceive());
        scheduledTransfer.setAmount(scheduledTransferDTO.amount());
        scheduledTransfer.setScheduledDate(scheduledTransferDTO.scheduledDate());

        return scheduledTransferRepository.save(scheduledTransfer);
    }

    public void deleteScheduledTransfer(UUID id) {
        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A transferência agendada com o ID fornecido não foi encontrada."));

        scheduledTransferRepository.delete(scheduledTransfer);
    }

    public ScheduledTransfer retryFailedTransfer(UUID id) {
        ScheduledTransfer transfer = scheduledTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada"));

        if (transfer.getStatus() != EnumStatus.FALHOU) {
            throw new IllegalArgumentException("A transferência não está no status 'FALHOU', não pode ser reprocessada.");
        }

        transfer.setScheduledDate(LocalDateTime.now());
        return scheduledTransferExecutor.execute(transfer);
    }

    public ScheduledTransfer cancelScheduledTransfer(UUID id) {
        ScheduledTransfer transfer = scheduledTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada"));

        if (transfer.getStatus() == EnumStatus.CONCLUIDA) {
            throw new IllegalArgumentException("Transferência já concluída, não pode ser cancelada.");
        }

        transfer.setStatus(EnumStatus.CANCELADA);
        return scheduledTransferRepository.save(transfer);
    }

    public List<ScheduledTransfer> getScheduledByUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("O usuario não fez nenhuma transferencia agendada ainda");
        }

        return scheduledTransferRepository.findAllByUser(new User(id));
    }

    public List<ScheduledTransfer> getFailedTransfers() {
        return scheduledTransferRepository.findByStatus(EnumStatus.FALHOU);
    }

    public ScheduledTransfer getTransferStatus(UUID id) {
        return scheduledTransferRepository.findStatusByid(id);

    }

    public List<ScheduledTransfer> getPendingTransfers(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        List<ScheduledTransfer> scheduledTransfers = user.getScheduledTransfer().stream()
                .filter(t -> t.getStatus().equals(EnumStatus.PENDENTE))
                .collect(Collectors.toList());

        if (scheduledTransfers.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma transferência pendente encontrada");
        }

        return scheduledTransfers;
    }


}
