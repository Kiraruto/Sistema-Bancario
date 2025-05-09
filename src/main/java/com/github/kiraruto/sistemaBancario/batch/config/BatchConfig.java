package com.github.kiraruto.sistemaBancario.batch.config;

import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SavingsAccountRepository savingsAccountRepository;
    private final JobLauncher jobLauncher;

    // Agendamento diário às 02:00 da manhã
    @Scheduled(cron = "0 0 2 * * *")
    public void runInterestCalculationJob() throws Exception {
        jobLauncher.run(interestCalculationJob(), new org.springframework.batch.core.JobParameters());
    }

    @Bean
    public Job interestCalculationJob() {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(interestCalculationStep())
                .build();
    }

    @Bean
    public Step interestCalculationStep() {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<SavingsAccount, SavingsAccount>chunk(50, transactionManager)
                .reader(interestReader())
                .processor(interestProcessor())
                .writer(interestWriter())
                .build();
    }

    @Bean
    public ListItemReader<SavingsAccount> interestReader() {
        List<SavingsAccount> contas = savingsAccountRepository.findAll();
        return new ListItemReader<>(contas);
    }

    @Bean
    public ItemProcessor<SavingsAccount, SavingsAccount> interestProcessor() {
        return conta -> {
            BigDecimal feeMonthly = new BigDecimal("0.005");
            BigDecimal rateDaily = feeMonthly.divide(new BigDecimal("30"), 10, RoundingMode.HALF_EVEN);
            BigDecimal saldo = conta.getBalance();
            BigDecimal jurosDoDia = saldo.multiply(rateDaily).setScale(2, RoundingMode.HALF_EVEN);

            if (jurosDoDia.compareTo(BigDecimal.ZERO) > 0) {
                conta.setBalance(saldo.add(jurosDoDia));
                System.out.printf("Conta %s: Juros de R$ %.2f aplicados. Novo saldo: R$ %.2f%n",
                        conta.getId(), jurosDoDia, conta.getBalance());
                return conta;
            }

            return null;
        };
    }

    @Bean
    public RepositoryItemWriter<SavingsAccount> interestWriter() {
        RepositoryItemWriter<SavingsAccount> writer = new RepositoryItemWriter<>();
        writer.setRepository(savingsAccountRepository);
        writer.setMethodName("save");
        return writer;
    }
}
