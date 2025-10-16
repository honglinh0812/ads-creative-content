package com.fbadsautomation.controller;

import com.fbadsautomation.dto.PromptValidationRequest;
import com.fbadsautomation.dto.PromptValidationResponse;
import com.fbadsautomation.service.PromptValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prompt")
@Tag(name = "Prompt Validation", description = "APIs for validating and improving ad prompts")
public class PromptValidationController {

    private static final Logger log = LoggerFactory.getLogger(PromptValidationController.class);

    @Autowired
    private PromptValidationService promptValidationService;

    @PostMapping("/validate")
    @Operation(summary = "Validate prompt quality", description = "Analyze prompt and provide quality score, issues, and improvements")
    public ResponseEntity<PromptValidationResponse> validatePrompt(
            @Valid @RequestBody PromptValidationRequest request,
            Authentication authentication) {

        log.info("User {} validating prompt (length: {})",
                authentication != null ? authentication.getName() : "anonymous",
                request.getPrompt().length());

        PromptValidationResponse response = promptValidationService.validatePrompt(
            request.getPrompt(),
            request.getAdType(),
            request.getLanguage(),
            request.getTargetAudience(),
            request.getIndustry()
        );

        return ResponseEntity.ok(response);
    }
}
