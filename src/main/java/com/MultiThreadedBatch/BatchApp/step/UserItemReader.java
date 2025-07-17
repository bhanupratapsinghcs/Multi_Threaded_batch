package com.MultiThreadedBatch.BatchApp.step;

import com.MultiThreadedBatch.BatchApp.entity.User;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;

public class UserItemReader extends FlatFileItemReader<User> {

    public UserItemReader() {
        setName("userItemReader");
        setLinesToSkip(1); // Skip header line
        setResource(new org.springframework.core.io.ClassPathResource("large_dataset.csv"));
        setLineMapper(getDefaultLineMapper());
    }

    private LineMapper<User> getDefaultLineMapper() {
        return new org.springframework.batch.item.file.mapping.DefaultLineMapper<User>() {{
            setLineTokenizer(new org.springframework.batch.item.file.transform.DelimitedLineTokenizer() {{
                setNames("id", "name", "date");
                setDelimiter(",");
            }});
            setFieldSetMapper(fieldSet -> User.builder()
                    .id(fieldSet.readInt("id"))
                    .name(fieldSet.readString("name"))
                    .date(fieldSet.readString("date"))
                    .build());
            setStrict(false); // Allow for missing fields
        }};
    }
}
