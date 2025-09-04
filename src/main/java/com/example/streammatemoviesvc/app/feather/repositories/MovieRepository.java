package com.example.streammatemoviesvc.app.feather.repositories;

import com.example.streammatemoviesvc.app.feather.models.dtos.ActorLatestMovies;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    Optional<Movie> findByTitleAndPosterImgURL(String cinemaRecTitle, String cinemaRecPosterImage);

    @Query(value = "SELECT count(*) FROM movies WHERE title ILIKE CONCAT('%', :movieName, '%')", nativeQuery = true)
    long findMoviesCountByTitleOrSearchTagContainingIgnoreCase(@Param("movieName") String movieName);

    @Query(value = "SELECT * FROM movies WHERE title ILIKE CONCAT('%', :movieName, '%')", nativeQuery = true)
    List<Movie> findByTitleOrSearchTagContainingIgnoreCase(@Param("movieName") String movieName);

    @Query(value = "SELECT id, title, poster_img_url, release_date, video_url FROM movies ORDER BY created_at DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> getThirthyMoviesRawData(@Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT id, title, poster_img_url, release_date, video_url FROM movies WHERE LOWER(genres) LIKE LOWER(CONCAT('%', :genre, '%')) ORDER BY created_at DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> findByGenreNextTwentyMovies(@Param("genre") String genre, @Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM movies WHERE LOWER(genres) LIKE LOWER(CONCAT('%', :genre, '%'))", nativeQuery = true)
    long findMoviesCountByGenre(@Param("genre") String genre);

    @Query(value =
            "SELECT id, comment_text, author_username, author_full_name, author_img_url, " +
                    "author_id, rating, created_at " +
                    "FROM movies_comments " +
                    "WHERE movie_id = :currentCinemaRecordId " +
                    "ORDER BY created_at DESC " +
                    "LIMIT 10 OFFSET :offset",
            nativeQuery = true)
    List<Object[]> getNext10Comments(@Param("offset") int offset,
                                     @Param("currentCinemaRecordId") UUID currentCinemaRecordId);

    @Query(value = "SELECT * FROM movies WHERE video_url = :videoURL", nativeQuery = true)
    Optional<Movie> findByVideoURL(@Param("videoURL") String videoURL);

    @Query(value = "SELECT \n" +
            "m.video_url AS videoURL,\n" +
            "m.poster_img_url AS posterURL,\n" +
            "m.title AS title,\n" +
            "m.tmdb_rating AS tmdbRating,\n" +
            "m.release_date AS releaseDate\n" +
            "FROM movies m\n" +
            "JOIN movies_actors ma ON ma.movie_id = m.id\n" +
            "JOIN actors a ON ma.actor_id = a.id\n" +
            "WHERE a.imdb_id = :imdb_id;", nativeQuery = true)
    List<Object[]> findActorLatestMovies(@Param("imdb_id") String imdb_id);
}