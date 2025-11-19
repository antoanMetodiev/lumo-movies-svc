package com.example.streammatemoviesvc.app.feather.services;

import com.example.streammatemoviesvc.app.commonData.utils.UtilMethods;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.TrendingMovie;
import com.example.streammatemoviesvc.app.feather.repositories.MovieRepository;
import com.example.streammatemoviesvc.app.feather.repositories.TrendingMoviesRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TrendingMoviesService {

    private final HttpClient httpClient;
    private final String TMDB_BASE_URL = System.getenv("TMDB_BASE_URL");
    private final String TMDB_API_KEY = System.getenv("TMDB_API_KEY");

    private final TrendingMoviesRepository trendingMoviesRepository;
    private final MovieService movieService;
    private final GenerateMoviesService generateMoviesService;
    private final MovieRepository movieRepository;

    @Autowired
    public TrendingMoviesService(HttpClient httpClient,
                                 TrendingMoviesRepository trendingMoviesRepository,
                                 MovieService movieService, GenerateMoviesService generateMoviesService,
                                 MovieRepository movieRepository) {

        this.httpClient = httpClient;
        this.trendingMoviesRepository = trendingMoviesRepository;
        this.movieService = movieService;
        this.generateMoviesService = generateMoviesService;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public void generateTrendingMovies() throws IOException, InterruptedException {
        removeTrendingMovies(); // this is first:
        final String URL = TMDB_BASE_URL + "/3/trending/movie/day?api_key=" + TMDB_API_KEY;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL)).build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);
            JsonArray results = jsonObject.get("results").getAsJsonArray();

            int count = 0;
            for (JsonElement currentMovie : results) {
                if (count >= 10) break; // само топ 8

                jsonObject = currentMovie.getAsJsonObject();

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

                Map<String, String> genresAndTagline = getGenresAndTagline(movieId);

                // Get: Trailer, Logo, Genres & Tagline:
                String trailerURL = getTrailerURL(movieId, title);
                String logoURL = getLogo(movieId, title);
                String genres = genresAndTagline.get("genres");
                String tagline = genresAndTagline.get("tagline");

                // Create Trending Movie:
                TrendingMovie movie = new TrendingMovie();
                movie.setMovieId(movieId);
                movie.setTitle(title);
                movie.setDescription(description);
                movie.setReleaseDate(releaseDate);
                movie.setBackgroundImg_URL(backgroundIMG);
                movie.setPosterImgURL(posterIMG);
                movie.setTmdbRating(movieRating);
                movie.setTrailerVideoURL(trailerURL);
                movie.setCreatedAt(Instant.now());
                movie.setLogoURL(logoURL);
                movie.setGenres(genres);
                movie.setSpecialText(tagline);
                movie.setSearchTag(title);

                // Save Trending Movie:
                saveTrendingMovie(movie);

                count++;
            }
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void saveTrendingMovie(TrendingMovie movie) {
        Optional<TrendingMovie> response = trendingMoviesRepository.findByMovieId(movie.getMovieId());
        if (response.isPresent()) return;

        // Подаваме заявка за да се съхраняват и в главната база данни.
        Thread.ofVirtual().start(() -> generateMoviesService.searchForMovies(movie.getTitle()));

        try {
            Thread.sleep(10_000);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        String videoURL = "https://vidsrc.icu/embed/movie/" + movie.getMovieId();
        Optional<Movie> isContained = movieRepository.findByVideoURL(videoURL);

        if (isContained.isPresent()) {
            trendingMoviesRepository.save(movie);
        }
    }

    public Map<String, String> getGenresAndTagline(final String movieId) throws IOException, InterruptedException {
        final Map<String, String> genresAndTagline = new HashMap<>();
        genresAndTagline.put("genres", "");
        genresAndTagline.put("tagline", "");

        final String detailsUrl = TMDB_BASE_URL + "/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY + "&language=en-US";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(detailsUrl)).build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);

            // Tagline
            String tagline = UtilMethods.getJsonValue(json, "tagline");

            // Genres
            JsonArray genresArray = json.getAsJsonArray("genres");
            StringBuilder genres = new StringBuilder();
            for (JsonElement g : genresArray) {
                JsonObject genreObj = g.getAsJsonObject();
                if (genres.length() > 0) genres.append(", ");
                genres.append(UtilMethods.getJsonValue(genreObj, "name"));
            }

            genresAndTagline.put("genres", genres.toString());
            genresAndTagline.put("tagline", tagline);

            return genresAndTagline;
        }

        return genresAndTagline;
    }

    public String getLogo(final String movieId, final String title) throws IOException, InterruptedException {
        // --- Вземаме Логото: ---
        String imagesUrl = TMDB_BASE_URL + "/3/movie/" + movieId + "/images?api_key=" + TMDB_API_KEY;
        HttpRequest imagesRequest = HttpRequest.newBuilder().uri(URI.create(imagesUrl)).build();
        HttpResponse<String> imagesResponse = this.httpClient.send(imagesRequest, HttpResponse.BodyHandlers.ofString());

        String titleLogo = "";
        if (imagesResponse.statusCode() == 200) {
            JsonObject imagesJson = new Gson().fromJson(imagesResponse.body(), JsonObject.class);
            JsonArray logos = imagesJson.getAsJsonArray("logos");

            for (JsonElement logoElem : logos) {
                JsonObject logoObj = logoElem.getAsJsonObject();
                String iso = UtilMethods.getJsonValue(logoObj, "iso_639_1");
                String filePath = UtilMethods.getJsonValue(logoObj, "file_path");

                if ("en".equalsIgnoreCase(iso) && filePath != null && !filePath.isEmpty()) {
                    titleLogo = "https://image.tmdb.org/t/p/original" + filePath;
                    break; // взимаме първото английско лого
                }
            }
        }

        System.out.println(title + " - Title Logo: " + titleLogo);
        return titleLogo;
    }

    public String getTrailerURL(final String movieId, final String title) throws IOException, InterruptedException {
        // --- Вземаме трейлъра: ---
        String trailerUrl = TMDB_BASE_URL + "/3/movie/" + movieId + "/videos?api_key=" + TMDB_API_KEY + "&language=en-US";
        HttpRequest trailerRequest = HttpRequest.newBuilder().uri(URI.create(trailerUrl)).build();
        HttpResponse<String> trailerResponse = this.httpClient.send(trailerRequest, HttpResponse.BodyHandlers.ofString());

        String youtubeLink = "";
        if (trailerResponse.statusCode() == 200) {
            JsonObject trailerJson = new Gson().fromJson(trailerResponse.body(), JsonObject.class);
            JsonArray videos = trailerJson.get("results").getAsJsonArray();

            for (JsonElement videoElem : videos) {
                JsonObject videoObj = videoElem.getAsJsonObject();
                String type = UtilMethods.getJsonValue(videoObj, "type");
                String site = UtilMethods.getJsonValue(videoObj, "site");
                String key = UtilMethods.getJsonValue(videoObj, "key");

                if ("Trailer".equalsIgnoreCase(type) && "YouTube".equalsIgnoreCase(site)) {
                    youtubeLink = "https://www.youtube.com/watch?v=" + key;
                    break;
                }
            }
        }

        System.out.println(title + " - Trailer: " + youtubeLink);
        return youtubeLink;
    }

    public List<TrendingMovie> getTrendingMovies() {
        return trendingMoviesRepository.get6TrendingMovie();
    }

    public void removeTrendingMovies() {
        trendingMoviesRepository.deleteAll();
    }
}
