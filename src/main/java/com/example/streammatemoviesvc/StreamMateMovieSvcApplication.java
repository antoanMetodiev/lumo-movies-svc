package com.example.streammatemoviesvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class StreamMateMovieSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamMateMovieSvcApplication.class, args);
    }
}
