package com.fbadsautomation.model;

import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j

public class AdGenerationResponse {
    private List<AdContent> adVariations;
    private String status;
    private String message;
}
