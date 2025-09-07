package com.example.streammatemoviesvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableRetry
@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class StreamMateMovieSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamMateMovieSvcApplication.class, args);
    }
}
