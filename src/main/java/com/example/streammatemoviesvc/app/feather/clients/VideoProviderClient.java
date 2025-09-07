package com.example.streammatemoviesvc.app.feather.clients;

import com.example.streammatemoviesvc.app.configs.FeignConfig;
import com.example.streammatemoviesvc.app.feather.models.dtos.VideoProviderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Set;

//@FeignClient(name = "video-url-provider", url = "http://localhost:11000", configuration = FeignConfig.class)
@FeignClient(name = "video-url-provider", url = "https://videos-provider.onrender.com", configuration = FeignConfig.class)
public interface VideoProviderClient {

    @PostMapping(value = "/get-links", consumes = "application/json")
    Set<String> getAllVideoSrc(@RequestBody VideoProviderRequest videoProviderRequest);
}
