package com.MultiThreadedBatch.BatchApp.step;

import com.MultiThreadedBatch.BatchApp.entity.User;
import com.MultiThreadedBatch.BatchApp.repository.UserRepository;
import org.springframework.batch.item.data.RepositoryItemWriter;

public class UserItemWriter extends RepositoryItemWriter<User> {

    public UserItemWriter(UserRepository userRepository) {
        setRepository(userRepository);
        setMethodName("save");
    }
}
