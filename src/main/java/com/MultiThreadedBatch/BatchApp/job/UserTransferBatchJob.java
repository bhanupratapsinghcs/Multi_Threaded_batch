package com.MultiThreadedBatch.BatchApp.job;

import com.MultiThreadedBatch.BatchApp.entity.User;
import com.MultiThreadedBatch.BatchApp.listener.UserTransferJobListener;
import com.MultiThreadedBatch.BatchApp.partitioner.UserPartitioner;
import com.MultiThreadedBatch.BatchApp.repository.UserRepository;
import com.MultiThreadedBatch.BatchApp.step.UserItemProcesser;
import com.MultiThreadedBatch.BatchApp.step.UserItemReader;
import com.MultiThreadedBatch.BatchApp.step.UserItemWriter;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableBatchProcessing
public class UserTransferBatchJob {
    @Autowired
    private UserRepository userRepository;


    @Bean
    @Qualifier("userItemWriter")
    public ItemWriter<User> userItemWriter() {
        return new UserItemWriter(userRepository);
    }

    @Bean
    @Qualifier("userItemProcessor")
    public ItemProcessor<User, User> userItemProcessor() {
        return new UserItemProcesser();
    }

    @Bean
    @Qualifier("userItemReader")
    public ItemReader<User> userItemReader() {
        return new UserItemReader();
    }

    @Bean
    public Step userProcessingStep(StepBuilderFactory stepBuilderFactory,
                                   @Qualifier("userItemReader") ItemReader<User> userItemReader,
                                   @Qualifier("userItemProcessor") ItemProcessor<User, User> userItemProcessor,
                                   @Qualifier("userItemWriter") ItemWriter<User> userItemWriter) {
        return stepBuilderFactory.get("userProcessingStep")
                .<User, User>chunk(200) // Process 200 records at a time
                .reader(userItemReader)
                .processor(userItemProcessor)
                .writer(userItemWriter)
                .build();
    }

    @Bean
    public Partitioner userPartitioner() {
        return new UserPartitioner(); // Assuming UserPartitioner is defined elsewhere
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Set the core pool size
        executor.setMaxPoolSize(20); // Set the maximum pool size
        executor.setQueueCapacity(100); // Set the queue capacity
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // Handle rejected tasks
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskExecutorPartitionHandler taskExecutorPartitionHandler(@Qualifier("userProcessingStep")Step userProcessingStep,
                                                                      TaskExecutor taskExecutor) {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setGridSize(10); // Number of partitions
        partitionHandler.setStep(userProcessingStep);
        partitionHandler.setTaskExecutor(taskExecutor);
        return partitionHandler;
    }

    @Bean
    public Step partitionerStep(StepBuilderFactory stepBuilderFactory, Partitioner partitioner,
                                @Qualifier("userProcessingStep") Step userProcessingStep,
                                TaskExecutorPartitionHandler taskExecutorPartitionHandler) {
        return stepBuilderFactory.get("partitionerUserStep")
                .partitioner(userProcessingStep)
                .partitionHandler(taskExecutorPartitionHandler)
                .partitioner("userProcessingStep", partitioner)
                .build();
    }

    @Bean
    public Job userTransferJob(JobBuilderFactory jobBuilderFactory,
                               @Qualifier("partitionerStep") Step partitionerStep,
                               ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        return jobBuilderFactory.get("userTransferJob")
                .incrementer(new RunIdIncrementer())
                .flow(partitionerStep).on(ExitStatus.STOPPED.getExitCode())
                .to(partitionerStep).on(ExitStatus.COMPLETED.getExitCode()).end(ExitStatus.COMPLETED.getExitCode())
                .end()
                .listener(new UserTransferJobListener(threadPoolTaskExecutor))
                .build();

    }

}
