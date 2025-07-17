package com.MultiThreadedBatch.BatchApp.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public class UserTransferJobListener implements JobExecutionListener {
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public UserTransferJobListener(ThreadPoolTaskExecutor threadPoolExecutor) {
        this.threadPoolTaskExecutor = threadPoolExecutor;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job started: {} (ID: {})", jobExecution.getJobInstance().getJobName(), jobExecution.getJobInstance().getInstanceId());
        log.info("Thread pool size: {}, Active threads: {}, Queue size: {}",
                threadPoolTaskExecutor.getPoolSize(),
                threadPoolTaskExecutor.getActiveCount(),
                threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (BatchStatus.COMPLETED.equals(jobExecution.getStatus())) {
            log.info("Job completed successfully. (ID: {})", jobExecution.getJobInstance().getInstanceId());
        } else {
            log.warn("Job did not complete successfully. (ID: {})", jobExecution.getJobInstance().getInstanceId());
        }

        if (threadPoolTaskExecutor.getActiveCount() == 0) {
            log.info("No active threads. Initiating thread pool shutdown.");
            threadPoolTaskExecutor.shutdown();
            log.info("Thread pool shutdown complete.");
        } else {
            log.info("Active threads detected ({}). Monitoring before shutdown.", threadPoolTaskExecutor.getActiveCount());
            monitorAndShutdownExecutor();
        }
    }

    private void monitorAndShutdownExecutor() {
        new Thread(() -> {
            log.info("Started monitoring thread for thread pool shutdown.");
            try {
                while (threadPoolTaskExecutor.getActiveCount() > 0) {
                    log.info("Waiting for active threads to finish. Remaining: {}", threadPoolTaskExecutor.getActiveCount());
                    Thread.sleep(10000);
                }
            } catch (InterruptedException e) {
                log.error("Monitoring thread interrupted.", e);
                Thread.currentThread().interrupt();
            }
            log.info("All threads finished. Initiating thread pool shutdown.");
            threadPoolTaskExecutor.shutdown();
            log.info("Thread pool shutdown complete.");
        }).start();
    }
}