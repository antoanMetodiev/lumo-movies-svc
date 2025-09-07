package com.example.streammatemoviesvc.app.feather.controllers;

import com.example.streammatemoviesvc.app.feather.services.PlayerLinksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class PlayerLinksController {

    private final PlayerLinksService playerLinksService;

    @Autowired
    public PlayerLinksController(PlayerLinksService playerLinksService) {
        this.playerLinksService = playerLinksService;
    }

    @PostMapping(value = "/get-links")
    public ResponseEntity<Void> generateMorePlayerLinks() throws IOException, InterruptedException {
        playerLinksService.generateMorePlayerLinks();
        return ResponseEntity.ok().build();
    }
}
