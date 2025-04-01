package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.dto.ScheduledTransferDTO;
import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.service.ScheduledTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.github.kiraruto.sistemaBancario.utils.VerifyUUID.validateUUID;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;

    @GetMapping
    public ResponseEntity<List<ScheduledTransfer>> getScheduled() {
        List<ScheduledTransfer> scheduledTransfer = scheduledTransferService.getScheduled();
        return ResponseEntity.ok(scheduledTransfer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTransfer> getScheduledById(@PathVariable("id") String id) {
        ScheduledTransfer scheduledTransfer = scheduledTransferService.getScheduledById(validateUUID(id));
        return ResponseEntity.ok(scheduledTransfer);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ScheduledTransfer>> getScheduledByUser(@PathVariable String userId) {
        List<ScheduledTransfer> scheduledTransfers = scheduledTransferService.getScheduledByUser(validateUUID(userId));
        return ResponseEntity.ok(scheduledTransfers);
    }

    @GetMapping("/{id}/pending")
    public ResponseEntity<List<ScheduledTransfer>> getPendingTransfers(@PathVariable String id) {
        List<ScheduledTransfer> failedTransfers = scheduledTransferService.getPendingTransfers(validateUUID(id));
        return ResponseEntity.ok(failedTransfers);
    }

    @GetMapping("/failed")
    public ResponseEntity<List<ScheduledTransfer>> getFailedTransfers() {
        List<ScheduledTransfer> failedTransfers = scheduledTransferService.getFailedTransfers();
        return ResponseEntity.ok(failedTransfers);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<EnumStatus> getTransferStatus(@PathVariable String id) {
        ScheduledTransfer transfer = scheduledTransferService.getTransferStatus(validateUUID(id));
        return ResponseEntity.ok(transfer.getStatus());
    }

    @PostMapping
    public ResponseEntity<ScheduledTransfer> scheduleTransfer(@RequestBody @Valid ScheduledTransferDTO scheduledTransferDTO) {
        ScheduledTransfer scheduledTransfer = scheduledTransferService.createScheduledTransfer(scheduledTransferDTO);
        return ResponseEntity.ok(scheduledTransfer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledTransfer> updateScheduledTransfer(@PathVariable String id, @RequestBody @Valid ScheduledTransferDTO scheduledTransferDTO) {
        ScheduledTransfer updatedTransfer = scheduledTransferService.updateScheduledTransfer(validateUUID(id), scheduledTransferDTO);
        return ResponseEntity.ok(updatedTransfer);
    }

    @PutMapping("{id}/retry/")
    public ResponseEntity<ScheduledTransfer> retryFailedTransfer(@PathVariable String id) {
        ScheduledTransfer transfer = scheduledTransferService.retryFailedTransfer(validateUUID(id));
        return ResponseEntity.ok(transfer);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ScheduledTransfer> cancelScheduledTransfer(@PathVariable String id) {
        ScheduledTransfer transfer = scheduledTransferService.cancelScheduledTransfer(validateUUID(id));
        return ResponseEntity.ok(transfer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduledTransfer(@PathVariable String id) {
        scheduledTransferService.deleteScheduledTransfer(validateUUID(id));
        return ResponseEntity.noContent().build();
    }
}
