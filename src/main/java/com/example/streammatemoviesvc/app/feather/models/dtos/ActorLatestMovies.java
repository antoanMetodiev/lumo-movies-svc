package com.example.streammatemoviesvc.app.feather.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActorLatestMovies {
    private String imdb_id;
    private String videoURL;
    private String posterURL;
    private String title;
    private String tmdbRating;
    private String releaseDate;
    private String TYPE = "MOVIE"; // може да е default
}
