package com.example.streammatemoviesvc.app.heartbeat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeartBeat {

    @GetMapping("/heartbeat")
    public void heartbeat() {
        System.out.println("==>>>>>>>>>>>>  Heartbeat request received...!");
    }
}
