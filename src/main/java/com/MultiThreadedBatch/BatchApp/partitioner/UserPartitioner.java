package com.MultiThreadedBatch.BatchApp.partitioner;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.Map;

public class UserPartitioner implements Partitioner {
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ExecutionContext context = new ExecutionContext();
        context.putInt("partitionSize", gridSize);

        // Create a map to hold the partitions
        Map<String, ExecutionContext> partitionMap = new java.util.HashMap<>();

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext partitionContext = new ExecutionContext();
            partitionContext.putInt("partitionNumber", i);
            partitionMap.put("partition" + i, partitionContext);
        }

        return partitionMap;
    }
}
