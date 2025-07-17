package com.MultiThreadedBatch.BatchApp.step;

import com.MultiThreadedBatch.BatchApp.entity.User;
import org.springframework.batch.item.ItemProcessor;

public class UserItemProcesser implements ItemProcessor<User, User> {
    @Override
    public User process(User user) throws Exception {
        return user;
    }
}
