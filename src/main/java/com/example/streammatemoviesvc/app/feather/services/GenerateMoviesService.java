package com.example.streammatemoviesvc.app.feather.services;

import com.example.streammatemoviesvc.app.commonData.models.entities.Actor;
import com.example.streammatemoviesvc.app.commonData.models.enums.ImageType;
import com.example.streammatemoviesvc.app.commonData.repositories.ActorRepository;
import com.example.streammatemoviesvc.app.commonData.utils.UtilMethods;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieImage;
import com.example.streammatemoviesvc.app.feather.repositories.MovieCommentRepository;
import com.example.streammatemoviesvc.app.feather.repositories.MovieRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

@Service
public class GenerateMoviesService {
    private final String TMDB_API_KEY = System.getenv("TMDB_API_KEY");
    private final String TMDB_BASE_URL = System.getenv("TMDB_BASE_URL");

    private final HttpClient httpClient;
    private final ActorRepository actorRepository;
    private final MovieRepository movieRepository;
    private final MovieCommentRepository movieCommentRepository;

    private final TransactionTemplate transactionTemplate;
    private final Executor asyncExecutor;

    @Autowired
    public GenerateMoviesService(HttpClient httpClient,
                                 ActorRepository actorRepository,
                                 MovieRepository movieRepository,
                                 MovieCommentRepository movieCommentRepository,
                                 TransactionTemplate transactionTemplate,
                                 Executor asyncExecutor) {

        this.httpClient = httpClient;
        this.actorRepository = actorRepository;
        this.movieRepository = movieRepository;
        this.movieCommentRepository = movieCommentRepository;
        this.transactionTemplate = transactionTemplate;
        this.asyncExecutor = asyncExecutor;
    }

    // Нека създава нова транзакция, а не да взима съществуващата:
//    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void searchForMovies(String movieName) {
        if (movieName.trim().isEmpty()) return;

        try {
            String encodedMovieName = URLEncoder.encode(movieName, StandardCharsets.UTF_8);
            String searchQuery = TMDB_BASE_URL + "/3/search/movie?api_key=" + TMDB_API_KEY + "&query=" + encodedMovieName;

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchQuery)).build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Process Movies:
            processMovies(response);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public void processMovies(HttpResponse<String> response) {

        try {

            if (response.statusCode() == 200) {
                JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);

                // JsonArray results = jsonObject.get("results").getAsJsonArray();

                JsonArray results;
                if (jsonObject.has("results") && jsonObject.get("results").isJsonArray()) {
                    // Ако има "results" => нормално поведение
                    results = jsonObject.get("results").getAsJsonArray();
                } else {
                    // НЯМА "results", следователно jsonObject е единичен филм
                    results = new JsonArray();
                    results.add(jsonObject);
                }

                for (JsonElement currentMovie : results) {

                    jsonObject = currentMovie.getAsJsonObject();
                    Movie movie = new Movie();

                    String movieId = UtilMethods.getJsonValue(jsonObject, "id");
                    String title = UtilMethods.getJsonValue(jsonObject, "title");
                    String description = UtilMethods.getJsonValue(jsonObject, "overview");
                    String releaseDate = UtilMethods.getJsonValue(jsonObject, "release_date");
                    String backgroundIMG = UtilMethods.getJsonValue(jsonObject, "backdrop_path");
                    String posterIMG = UtilMethods.getJsonValue(jsonObject, "poster_path");
                    String movieRating = UtilMethods.getJsonValue(jsonObject, "vote_average");

                    // Checks:
                    if (posterIMG.trim().isEmpty()) continue;
                    if (releaseDate.trim().isEmpty()) continue;
                    if (LocalDate.parse(releaseDate).isAfter(LocalDate.now())) continue;
                    if (LocalDate.parse(releaseDate).getYear() < 2000) continue;
                    if (movieRating.equals("0.0")) continue;

                    String VidURL = "https://vidsrc.icu/embed/movie/" + movieId;
                    String castURL = TMDB_BASE_URL + "/3/movie/" + movieId + "/credits" + "?api_key=" + TMDB_API_KEY;

                    UtilMethods utilMethods = new UtilMethods();

                    // Стартираме асинхронни операции:
                    CompletableFuture<List<Actor>> asyncActors = utilMethods.extractActors(
                            castURL, this.httpClient, TMDB_BASE_URL, TMDB_API_KEY, asyncExecutor);
                    CompletableFuture<Boolean> extractedImages = extractImagesAsync(movieId, movie);
                    CompletableFuture<Boolean> extractGenresAndTaglineAsync = extractGenresAndTaglineAsync(movieId, title, movie);

                    // Изчакваме резултатите
                    List<Actor> actors = asyncActors.get();
                    addAllCast(actors, movie);
                    if (actors.isEmpty()) continue;
                    if (!extractedImages.get()) continue;
                    if (!extractGenresAndTaglineAsync.get()) continue;

                    // Запазвам крайният обект:
                    movie.setVideoURL(VidURL).setSearchTag(title).setTitle(title).setDescription(description)
                            .setReleaseDate(releaseDate).setBackgroundImg_URL(backgroundIMG)
                            .setPosterImgURL(posterIMG).setTmdbRating(movieRating)
                            .setCreatedAt(Instant.now());

                    saveMovie(VidURL, movie);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Async
    public CompletableFuture<Boolean> extractImagesAsync(String movieId, Movie movie) {
        String searchQuery = TMDB_BASE_URL + "/3/movie/" + movieId + "/images?api_key=" + TMDB_API_KEY;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchQuery)).build();

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);
                    JsonArray backdropsJsonAr = jsonObject.getAsJsonArray("backdrops");
                    JsonArray postersJsonAr = jsonObject.getAsJsonArray("posters");

                    List<MovieImage> allBackdropImgsFuture = extractDetailsImages(backdropsJsonAr, ImageType.BACKDROP, 29);
                    List<MovieImage> allPosterImages = extractDetailsImages(postersJsonAr, ImageType.POSTER, 8);

                    List<MovieImage> allImages = new ArrayList<>();
                    allImages.addAll(allBackdropImgsFuture);
                    allImages.addAll(allPosterImages);

                    if (allImages.size() < 4) return false;
                    movie.addAllImages(allImages);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }

            return true;
        }, asyncExecutor);
    }

    @Async
    public CompletableFuture<Boolean> extractGenresAndTaglineAsync(String movieId, String encodedMovieName, Movie movie) {
        String searchQuery = TMDB_BASE_URL + "/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchQuery)).build();

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);

                    String specialText = UtilMethods.getJsonValue(jsonObject, "tagline");
                    JsonElement genres = jsonObject.get("genres");
                    StringBuilder genresString = new StringBuilder();
                    genres.getAsJsonArray().forEach(genre -> {
                        genresString.append(UtilMethods.getJsonValue(genre.getAsJsonObject(), "name")).append(",");
                    });

                    if (genresString.isEmpty()) return false;
                    movie.setSpecialText(specialText).setGenres(genresString.toString());
                }

            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }

            return true;
        }, asyncExecutor);
    }

    public List<MovieImage> extractDetailsImages(JsonArray backdropsJsonAr, ImageType imageType, int limit) {
        if (backdropsJsonAr == null || imageType == null) {
            return new ArrayList<>();
        }

        List<MovieImage> backdropImages = new ArrayList<>();

        int count = 0;
        for (JsonElement jsonElement : backdropsJsonAr) {
            MovieImage image = new MovieImage();

            if (imageType.equals(ImageType.BACKDROP)) image.setImageType(ImageType.BACKDROP);
            else image.setImageType(ImageType.POSTER);

            backdropImages.add(image.setImageURL(jsonElement.getAsJsonObject().get("file_path")
                    .getAsString()));

            if (count++ == limit) break;
        }

        return backdropImages;
    }

    public void addAllCast(List<Actor> allCast, Movie movie) {
        int count = 0;

        for (Actor actor : allCast) {
            final String imdbId = actor.getImdbId();

            Optional<Actor> existingActor = this.actorRepository
                    .findByIMDB_ID(imdbId);

            if (existingActor.isPresent()) {
                actor = existingActor.get();
            }

            // Добавяме връзката между актьора и филма
            if (!movie.getCastList().contains(actor)) {
                movie.getCastList().add(actor);
            }

            // Добавяме филма към списъка на актьора
            if (!actor.getMoviesParticipations().contains(movie)) {
                actor.getMoviesParticipations().add(movie);
            }

            if (count++ == 20) return;
        }
    }


    public void saveMovie(String videoURL, Movie movie) {
        Optional<Movie> cinemaRecResponse = this.movieRepository.findByVideoURL(videoURL);

        if (cinemaRecResponse.isEmpty()) {
            // "Присвояваме" актьорите към текущата сесия
            List<Actor> managedActors = new ArrayList<>();
            for (Actor actor : movie.getCastList()) {
                if (actor.getId() != null) {
                    Actor managedActor = this.actorRepository.findById(actor.getId()).orElse(actor);
                    managedActors.add(managedActor);
                } else {
                    // Ако няма ID, значи е нов – го запазваме, за да получим ID и управляван екземпляр
                    managedActors.add(this.actorRepository.save(actor));
                }
            }
            movie.setCastList(managedActors);
            this.movieRepository.save(movie);
        }
    }
}
