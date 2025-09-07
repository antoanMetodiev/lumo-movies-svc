package com.example.streammatemoviesvc.app.feather.services;

import com.example.streammatemoviesvc.app.feather.clients.VideoProviderClient;
import com.example.streammatemoviesvc.app.feather.models.dtos.VideoProviderRequest;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.repositories.MovieRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class PlayerLinksService {

    private final HttpClient httpClient;

    private final VideoProviderClient videoProviderClient;
    private final MovieService movieService;
    private final MovieRepository movieRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PlayerLinksService(HttpClient httpClient,
                              VideoProviderClient videoProviderClient,
                              MovieService movieService,
                              MovieRepository movieRepository,
                              ObjectMapper objectMapper) {

        this.httpClient = httpClient;
        this.videoProviderClient = videoProviderClient;
        this.movieService = movieService;
        this.movieRepository = movieRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public CompletableFuture<Void> generateMorePlayerLinks() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<Movie> movies = movieRepository.getMoviesWithoutVideoUrls();
                System.out.println("🎬 Намерени филми без видео: " + movies.size());

                for (Movie movie : movies) {
                    System.out.println("+=======>>>>  ТЪРСЯТ СЕ ЗА ФИЛМ  =>  " + movie.getTitle() + " - " + movie.getReleaseDate());
                    System.out.println();
                    String year = movie.getReleaseDate().substring(0, 4);

                    VideoProviderRequest request = VideoProviderRequest.builder()
                            .title(movie.getTitle())
                            .releaseYear(year)
                            .build();

                    try {
                        Set<String> allVideoUrls = videoProviderClient.getAllVideoSrc(request);
                        System.out.println(allVideoUrls);

                        // 👀 Дебъг лог
                        System.out.println("✅ Получени URL-и за " + movie.getTitle() + ": " + allVideoUrls);

                        movie.setVideoURLs(new ArrayList<>()); // иначе ми e null, защото ги създавах ръчно, а не с hibernate
                        if (allVideoUrls.isEmpty()) {
                            movie.getVideoURLs().add("NOT ALLOWED!");
                        }

                        allVideoUrls.forEach(url -> movie.getVideoURLs().add(url));
                        movieRepository.save(movie);

                    } catch (Exception e) {
                        System.err.println("⚠️ Грешка при обработка на " + movie.getTitle() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                System.err.println("🔥 Критична грешка при генериране на линкове: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
