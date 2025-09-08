package com.example.streammatemoviesvc.app.commonData.repositories;

import com.example.streammatemoviesvc.app.commonData.models.entities.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActorRepository extends JpaRepository<Actor, UUID> {

    @Query(value = "SELECT * FROM actors WHERE imdb_id = :imdbId;", nativeQuery = true)
    Optional<Actor> findByIMDB_ID(@Param("imdbId") String imdbId);
}
