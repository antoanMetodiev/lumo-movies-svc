package com.example.streammatemoviesvc.app.feather.controllers;

import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.TrendingMovie;
import com.example.streammatemoviesvc.app.feather.services.TrendingMoviesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TrendingMoviesController {

    private final TrendingMoviesService trendingMoviesService;

    @Autowired
    public TrendingMoviesController(TrendingMoviesService trendingMoviesService) {
        this.trendingMoviesService = trendingMoviesService;
    }

    @GetMapping("/get-trending-movies")
    public List<TrendingMovie> getTrendingMovies() {
        return trendingMoviesService.getTrendingMovies();
    }

    @PostMapping("/find-new-trending-movies")
    public ResponseEntity<Void> findNewTrendingMovies() {

        try {
            trendingMoviesService.generateTrendingMovies();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
