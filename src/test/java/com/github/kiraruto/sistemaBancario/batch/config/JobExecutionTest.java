package com.github.kiraruto.sistemaBancario.batch.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JobExecutionTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job job;

    @Mock
    private JobExecution jobExecution;

    @Before
    public void setUp() throws Exception {
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(jobExecution);
    }

    @Test
    public void testJobExecutionSuccess() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("param1", "value1")
                .toJobParameters();

        JobExecution executedJobExecution = jobLauncher.run(job, jobParameters);

        assertEquals(ExitStatus.COMPLETED, executedJobExecution.getExitStatus());

        verify(jobLauncher).run(any(Job.class), eq(jobParameters));
    }

    @Test
    public void testJobExecutionFailure() throws Exception {
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.FAILED);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("param1", "invalid_value")
                .toJobParameters();

        JobExecution executedJobExecution = jobLauncher.run(job, jobParameters);

        assertEquals(ExitStatus.FAILED, executedJobExecution.getExitStatus());
    }
}
