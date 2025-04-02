package com.github.kiraruto.sistemaBancario.config;

import com.github.kiraruto.sistemaBancario.service.ScheduledTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class InterestJobSchedulerConfig {

    private final ScheduledTransferService scheduledTransferService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void processScheduledTransfers() {
        try {
            scheduledTransferService.processScheduledTransfers();
            log.info("Transferências agendadas processadas com sucesso!");
        } catch (Exception e) {
            log.error("Falha ao processar transferências agendadas: {}", e.getMessage());
        }
    }
}