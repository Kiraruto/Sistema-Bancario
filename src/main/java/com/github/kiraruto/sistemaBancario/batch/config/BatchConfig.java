package com.github.kiraruto.sistemaBancario.batch.config;

import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job interestCalculationJob(Step interestCalculationStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(interestCalculationStep)
                .build();
    }

    @Bean
    public Step interestCalculationStep(Tasklet interestTasklet) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .tasklet(interestTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet interestTasklet(SavingsAccountRepository savingsAccountRepository) {
        return (contribution, chunkContext) -> {
            System.out.println("Executando cálculo de juros...");

            BigDecimal feeMonthly = new BigDecimal("0.005");
            BigDecimal rateDaily = feeMonthly.divide(new BigDecimal("30"), 10, RoundingMode.HALF_EVEN);

            List<SavingsAccount> accounts = savingsAccountRepository.findAll();
            List<SavingsAccount> accountsToUpdate = new ArrayList<>();

            for (SavingsAccount account : accounts) {
                BigDecimal saldo = account.getBalance();
                BigDecimal jurosDoDia = saldo.multiply(rateDaily).setScale(2, RoundingMode.HALF_EVEN);

                if (jurosDoDia.compareTo(BigDecimal.ZERO) > 0) {
                    account.setBalance(saldo.add(jurosDoDia));
                    accountsToUpdate.add(account);

                    System.out.printf("✅ Conta %s: Juros de R$ %.2f aplicados. Novo saldo: R$ %.2f%n",
                            account.getId(), jurosDoDia, account.getBalance());
                }
            }

            if (!accountsToUpdate.isEmpty()) {
                savingsAccountRepository.saveAll(accountsToUpdate);
            }

            return RepeatStatus.FINISHED;
        };
    }
}
