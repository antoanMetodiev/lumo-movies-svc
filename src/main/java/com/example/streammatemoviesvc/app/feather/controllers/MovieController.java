package com.example.streammatemoviesvc.app.feather.controllers;

import com.example.streammatemoviesvc.app.feather.models.dtos.ActorLatestMovies;
import com.example.streammatemoviesvc.app.feather.models.dtos.CinemaRecordResponse;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.services.GenerateMoviesService;
import com.example.streammatemoviesvc.app.feather.services.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class MovieController {

    private final MovieService movieService;
    private final GenerateMoviesService generateMoviesService;

    @Autowired
    public MovieController(MovieService movieService, GenerateMoviesService generateMoviesService) {
        this.movieService = movieService;
        this.generateMoviesService = generateMoviesService;
    }

    @PostMapping("/add-movies-by-actor/{imdb_id}")
    public void addMoviesByActor(@PathVariable(name = "imdb_id") String imdb_id) {

        try {
            movieService.addMoviesByActor(imdb_id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/search-movies-matching-results/{searchTitle}")
    public List<CinemaRecordResponse> searchMoviesMatchingResults(@PathVariable(value = "searchTitle") String searchTitle) {
        List<CinemaRecordResponse> cinemaRecordResponses = movieService.searchMoviesMatchingResults(searchTitle);
        return cinemaRecordResponses;
    }

    @GetMapping("/actor/latest-movies/{imdb_id}")
    public List<ActorLatestMovies> getActorLatestMovies(@PathVariable(name = "imdb_id") String imdb_id) {
        return movieService.getActorLatestMovies(imdb_id);
    }

    @GetMapping("/get-searched-movies-count")
    public long getSearchedMoviesCount(@RequestParam String title) {
        return this.movieService.getSearchedMoviesCount(title);
    }

    @GetMapping("/get-movies-by-title")
    public List<CinemaRecordResponse> getMoviesByTitle(@RequestParam String title) {
        return this.movieService.getMoviesByTitle(title);
    }

    @GetMapping("/get-movies-count-by-genre")
    public long findMoviesCountByGenre(@RequestParam String genres) {
        return this.movieService.findMoviesCountByGenre(genres);
    }

    @GetMapping("/get-next-twenty-movies-by-genre")
    public List<CinemaRecordResponse> getNextTwentyMoviesByGenre(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 @RequestParam String receivedGenre) {

        return movieService.getNextTwentyMoviesByGenre(receivedGenre, PageRequest.of(page, size));  // Предаваме жанра и Pageable на сървиса
    }

    @GetMapping("/get-movie-details")
    public Movie getConcreteMovieDetails(@RequestParam String movieId) {
        Movie movie = this.movieService.getConcreteMovieDetails(movieId);
        System.out.println();
        return movie;
    }

    @GetMapping("/get-next-thirty-movies")
    public List<CinemaRecordResponse> getEveryThirtyMovies(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {

        System.out.println("Thread: " + Thread.currentThread() +
                " | Virtual: " + Thread.currentThread().isVirtual());

        Pageable pageable = PageRequest.of(page, size);
        Page<CinemaRecordResponse> everyThirtyMovies = movieService.getEveryThirtyMovies(pageable);
        System.out.println("Requested page: " + page + ", size: " + size); // Дебъгване

        List<CinemaRecordResponse> movies = new ArrayList<>();
        everyThirtyMovies.get().forEach(movies::add);
        return movies;
    }

    @PostMapping("/search-movies")
    public void searchMovies(@RequestBody String title) throws IOException, InterruptedException {
        System.out.println("====>>> Търся Филми...!!!");
        generateMoviesService.searchForMovies(title);
    }

    @GetMapping("/get-all-movies-count")
    public long getAllMoviesCount() {
        return this.movieService.getAllMoviesCount();
    }
}