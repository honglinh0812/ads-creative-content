package com.fbadsautomation.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdGenerationResponse {
    private List<AdContent> adVariations;
    private String status;
    private String message;
}
