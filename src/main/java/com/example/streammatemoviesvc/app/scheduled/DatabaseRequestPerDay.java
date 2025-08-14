package com.example.streammatemoviesvc.app.scheduled;

import com.example.streammatemoviesvc.app.feather.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabaseRequestPerDay {

    private final MovieRepository movieRepository;

    @Autowired
    public DatabaseRequestPerDay(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Scheduled(cron = "0 0 0,12 * * ?", zone = "Europe/Sofia")
    public void heartBeatSelf() {
        try {
            movieRepository.count();
            System.out.println("=====>>>>>>>>>>>>>  Database Ping Request..!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
