package com.example.streammatemoviesvc.app.feather.controllers;

import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import com.example.streammatemoviesvc.app.feather.services.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
public class MovieCommentsController {

    private final MovieService movieService;

    @Autowired
    public MovieCommentsController(MovieService movieService) {
        this.movieService = movieService;
    }

    @DeleteMapping("/delete-movie-comment")
    public void deleteMovieComment(@RequestParam String commentId,
                                   @RequestParam String movieId) {

        this.movieService.deleteMovieComment(commentId, movieId);
    }

    @GetMapping("/get-next-10-movie-comments")
    public List<MovieComment> getNext10Comments(@RequestParam int order,
                                                @RequestParam String currentCinemaRecordId) {

        return this.movieService.getNext10Comments(order, UUID.fromString(currentCinemaRecordId));
    }

    @PostMapping("/post-movie-comment")
    public void postComment(@RequestParam String authorUsername,
                            @RequestParam String authorFullName,
                            @RequestParam String authorImgURL,
                            @RequestParam String commentText,
                            @RequestParam double rating,
                            @RequestParam String createdAt,
                            @RequestParam String authorId,
                            @RequestParam String movieId) {

        this.movieService.postComment(authorUsername, authorFullName, authorImgURL, commentText, rating, createdAt, authorId, movieId);
    }
}
