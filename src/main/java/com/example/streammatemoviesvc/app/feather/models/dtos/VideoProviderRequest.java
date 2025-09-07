package com.example.streammatemoviesvc.app.feather.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VideoProviderRequest {
    private String title;
    private String releaseYear;
}
