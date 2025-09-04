package com.example.streammatemoviesvc.app.feather.repositories;

import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.TrendingMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendingMoviesRepository extends JpaRepository<TrendingMovie, UUID> {
    Optional<TrendingMovie> findByMovieId(String movieId);

    @Query(value = "SELECT * FROM trending_movies LIMIT 9;", nativeQuery = true)
    List<TrendingMovie> get6TrendingMovie();
}
