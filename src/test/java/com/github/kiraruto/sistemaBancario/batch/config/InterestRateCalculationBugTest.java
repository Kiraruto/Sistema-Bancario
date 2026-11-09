package com.github.kiraruto.sistemaBancario.batch.config;
import com.github.kiraruto.sistemaBancario.batch.config.BatchConfig;

import com.github.kiraruto.sistemaBancario.config.InterestJobSchedulerConfig;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Candidato E: job de juros com taxa fixa errada e agendamento duplicado.
 *
 * Contrato esperado: o calculo diario deve usar o interestRate real da conta
 * (SavingsAccount.interestRate = 0.006743) e o job deve ter apenas UM agendador.
 * O codigo atual usa 0.005 fixo no processor e agenda o mesmo job em BatchConfig
 * (02:00) e InterestJobSchedulerConfig (23:59).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterestRateCalculationBugTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    private BatchConfig batchConfig;

    @BeforeEach
    void setUp() {
        batchConfig = new BatchConfig(jobRepository, transactionManager, savingsAccountRepository);
    }

    @Test
    void shouldUseAccountInterestRateInsteadOfHardcodedFee() throws Exception {
        SavingsAccount account = new SavingsAccount();
        account.setBalance(BigDecimal.valueOf(1000));
        account.setInterestRate(BigDecimal.valueOf(0.006743));

        SavingsAccount processed = batchConfig.interestProcessor().process(account);

        assertNotNull(processed, "Conta com saldo positivo deveria ser processada");
        assertEquals(
                0,
                new BigDecimal("1000.22").compareTo(account.getBalance()),
                "Juros deveriam usar interestRate da conta: 1000 * (0.006743/30) = 0.22"
        );
    }

    @Test
    void interestJobShouldHaveOnlyOneScheduler() {
        long schedulers = Stream.of(BatchConfig.class, InterestJobSchedulerConfig.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(this::isInterestJobScheduler)
                .count();

        assertEquals(
                1,
                schedulers,
                "O job interestCalculationJob nao deveria ser agendado duas vezes"
        );
    }

    private boolean isInterestJobScheduler(Method method) {
        return method.isAnnotationPresent(Scheduled.class)
                && method.getName().equals("runInterestCalculationJob");
    }
}
