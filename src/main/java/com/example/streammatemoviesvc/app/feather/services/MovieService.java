package com.example.streammatemoviesvc.app.feather.services;

import com.example.streammatemoviesvc.app.commonData.repositories.ActorRepository;
import com.example.streammatemoviesvc.app.feather.models.dtos.ActorLatestMovies;
import com.example.streammatemoviesvc.app.feather.models.dtos.CinemaRecordResponse;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import com.example.streammatemoviesvc.app.feather.repositories.MovieCommentRepository;
import com.example.streammatemoviesvc.app.feather.repositories.MovieRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class MovieService {
    private final String TMDB_API_KEY = System.getenv("TMDB_API_KEY");
    private final String TMDB_BASE_URL = System.getenv("TMDB_BASE_URL");

    private final HttpClient httpClient;
    private final GenerateMoviesService generateMoviesService;
    private final ActorRepository actorRepository;
    private final MovieRepository movieRepository;
    private final MovieCommentRepository movieCommentRepository;

    private final TransactionTemplate transactionTemplate;
    private final Executor asyncExecutor;

    @Autowired
    public MovieService(HttpClient httpClient, GenerateMoviesService generateMoviesService,
                        ActorRepository actorRepository,
                        MovieRepository movieRepository,
                        MovieCommentRepository movieCommentRepository,
                        TransactionTemplate transactionTemplate,
                        Executor asyncExecutor) {

        this.httpClient = httpClient;
        this.generateMoviesService = generateMoviesService;
        this.actorRepository = actorRepository;
        this.movieRepository = movieRepository;
        this.movieCommentRepository = movieCommentRepository;
        this.transactionTemplate = transactionTemplate;
        this.asyncExecutor = asyncExecutor;
    }

    public void addMoviesByActor(final String imdbId) throws IOException, InterruptedException {
        // === 1. Намираш person_id през /find ===
        final String findUrl = String.format(
                "https://api.themoviedb.org/3/find/%s?external_source=imdb_id&api_key=%s",
                imdbId, TMDB_API_KEY
        );

        HttpRequest findRequest = HttpRequest.newBuilder().uri(URI.create(findUrl)).build();
        HttpResponse<String> findResponse = httpClient.send(findRequest, HttpResponse.BodyHandlers.ofString());

        JsonObject findJson = new Gson().fromJson(findResponse.body(), JsonObject.class);
        JsonArray personResults = findJson.getAsJsonArray("person_results");

        if (personResults == null || personResults.isEmpty()) {
            System.out.println("No person found for imdbId = " + imdbId);
            return;
        }

        int personId = personResults.get(0).getAsJsonObject().get("id").getAsInt();

        // === 2. Взимаш само филмовите кредити ===
        final String movieCreditsUrl = String.format(
                "https://api.themoviedb.org/3/person/%d/movie_credits?api_key=%s",
                personId, TMDB_API_KEY
        );

        HttpRequest movieRequest = HttpRequest.newBuilder().uri(URI.create(movieCreditsUrl)).build();
        HttpResponse<String> movieResponse = httpClient.send(movieRequest, HttpResponse.BodyHandlers.ofString());

        JsonObject movieJson = new Gson().fromJson(movieResponse.body(), JsonObject.class);
        JsonArray castMovies = movieJson.getAsJsonArray("cast");

        if (castMovies == null) {
            System.out.println("No cast movies found.");
            return;
        }

        // === 3. Обработка на cast ===

        for (JsonElement element : castMovies) {
            JsonObject movie = element.getAsJsonObject();

            int movieId = movie.get("id").getAsInt();
            String title = movie.has("title") ? movie.get("title").getAsString()
                    : movie.has("original_title") ? movie.get("original_title").getAsString()
                    : "N/A";

            String character = movie.has("character") ? movie.get("character").getAsString() : "";
            System.out.println("Movie: " + title + " (" + movieId + ") as " + character);

            try {
                // 1. Взимам точния филм:
                String movieUrl = TMDB_BASE_URL + "/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(movieUrl)).build();
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                transactionTemplate.execute(status -> {
                    generateMoviesService.processMovies(response);
                    return null;
                });

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public Page<CinemaRecordResponse> getEveryThirtyMovies(Pageable pageable) {
        int size = pageable.getPageSize();
        int offset = pageable.getPageNumber() * size;
        List<Object[]> rawData = movieRepository.getThirthyMoviesRawData(size, offset);

        List<CinemaRecordResponse> dtos = rawData.stream().map(obj ->
                new CinemaRecordResponse(
                        (UUID) obj[0],
                        (String) obj[1],  // title
                        (String) obj[2],  // posterImgURL
                        (String) obj[3],  // releaseDate
                        (String) obj[4]
                )
        ).toList();

        return new PageImpl<>(dtos, pageable, dtos.size());
    }

    public long getAllMoviesCount() {
        return this.movieRepository.count();
    }

    public Movie getConcreteMovieDetails(String videoURL) {
        videoURL = "https://vidsrc.icu/embed/movie/" + videoURL;
        return this.movieRepository.findByVideoURL(videoURL).orElseThrow();
    }

    public List<CinemaRecordResponse> getMoviesByTitle(String title) {
        List<Object[]> response = this.movieRepository.findByTitleOrSearchTagContainingIgnoreCase(title);

        List<CinemaRecordResponse> dtos = response.stream().map(obj ->
                new CinemaRecordResponse(
                        (UUID) obj[0],
                        (String) obj[1],  // title
                        (String) obj[2],  // posterImgURL
                        (String) obj[3],   // releaseDate
                        (String) obj[4]
                )
        ).toList();

        return dtos;
    }

    public long findMoviesCountByGenre(String genre) {
        return this.movieRepository.findMoviesCountByGenre(genre);
    }

    public List<CinemaRecordResponse> getNextTwentyMoviesByGenre(String genre, Pageable pageable) {
        int size = pageable.getPageSize();
        int offset = pageable.getPageNumber() * size;

        List<Object[]> moviesByGenres = movieRepository.findByGenreNextTwentyMovies(genre, size, offset);
        List<CinemaRecordResponse> dtos = moviesByGenres.stream().map(obj ->
                new CinemaRecordResponse(
                        (UUID) obj[0],
                        (String) obj[1],  // title
                        (String) obj[2],  // posterImgURL
                        (String) obj[3],   // releaseDate
                        (String) obj[4]
                )
        ).toList();

        return dtos;
    }

    public long getSearchedMoviesCount(String title) {
        return this.movieRepository.findMoviesCountByTitleOrSearchTagContainingIgnoreCase(title);
    }

    @Transactional
    public void postComment(String authorUsername, String authorFullName,
                            String authorImgURL, String commentText, double rating,
                            String createdAt,
                            String authorId,
                            String movieId) {

        UUID id = UUID.fromString(movieId);
        Movie movie = this.movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie is not found!"));

        MovieComment comment = new MovieComment();
        comment.setAuthorUsername(authorUsername);
        comment.setAuthorFullName(authorFullName);
        comment.setAuthorImgURL(authorImgURL);
        comment.setCommentText(commentText);
        comment.setRating(rating);
        comment.setCreatedAt(createdAt);
        comment.setMovie(movie);
        comment.setAuthorId(UUID.fromString(authorId));

        movie.getMovieComments().add(comment);
        this.movieRepository.save(movie);
    }

    public List<MovieComment> getNext10Comments(int order, UUID currentCinemaRecordId) {
        int offset = (order - 1) * 10;  // Преобразуване на order в offset
        List<Object[]> next10Comments = this.movieRepository.getNext10Comments(offset, currentCinemaRecordId);

        List<MovieComment> movieComments = new ArrayList<>();

        for (Object[] comment : next10Comments) {
            MovieComment movieComment = new MovieComment();

            movieComment.setId((UUID) comment[0]);
            movieComment.setCommentText((String) comment[1]);
            movieComment.setAuthorUsername((String) comment[2]);
            movieComment.setAuthorFullName((String) comment[3]);
            movieComment.setAuthorImgURL((String) comment[4]);
            movieComment.setAuthorId((UUID) comment[5]);
            movieComment.setRating((Double) comment[6]);
            movieComment.setCreatedAt((String) comment[7]);

            movieComments.add(movieComment);
        }

        return movieComments;
    }

    @Transactional
    public void deleteMovieComment(String commentId, String movieId) {
        UUID currentMovieId = UUID.fromString(movieId);
        UUID currentCommentId = UUID.fromString(commentId);

        // Изтегляне на филма по ID
        Movie movie = this.movieRepository.findById(currentMovieId)
                .orElseThrow(() -> new RuntimeException("Movie not found!"));

        MovieComment commentToDelete = movie.getMovieComments().stream()
                .filter(comment -> comment.getId().equals(currentCommentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Comment not found!"));

        movie.getMovieComments().remove(commentToDelete);
        this.movieCommentRepository.delete(commentToDelete);

        this.movieRepository.save(movie);
    }

    public List<ActorLatestMovies> getActorLatestMovies(final String imdb_id) {
        List<Object[]> responseData = movieRepository.findActorLatestMovies(imdb_id);
        List<ActorLatestMovies> actorLatestMovies = new ArrayList<>();
        responseData.forEach(responseMovie -> {
            actorLatestMovies.add(ActorLatestMovies.builder()
                    .videoURL(responseMovie[0].toString())
                    .posterURL(responseMovie[1].toString())
                    .title(responseMovie[2].toString())
                    .tmdbRating(responseMovie[3].toString())
                    .releaseDate(responseMovie[4].toString())
                    .TYPE("MOVIE")
                    .build());
        });

        return actorLatestMovies;
    }

    public List<CinemaRecordResponse> searchMoviesMatchingResults(String searchTitle) {
        List<Object[]> response = movieRepository.searchMoviesMatchingResults(searchTitle);

        List<CinemaRecordResponse> dtos = response.stream().map(obj ->
                new CinemaRecordResponse(
                        (UUID) obj[0],
                        (String) obj[1],  // title
                        (String) obj[2],  // posterImgURL
                        (String) obj[3],   // releaseDate
                        (String) obj[4]
                )
        ).toList();

        return dtos;
    }
}
