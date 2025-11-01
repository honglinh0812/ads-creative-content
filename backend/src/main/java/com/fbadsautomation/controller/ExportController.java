package com.fbadsautomation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/export")
@CrossOrigin(origins = {"https://linhnh.site", "https://api.linhnh.site"})
@Tag(name = "Export", description = "Data export endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    @Operation(summary = "Export data", description = "Exports user data in CSV format")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data exported successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/data")
    public ResponseEntity<byte[]> exportData(Authentication authentication) {
        log.info("Exporting data for user: {}", authentication.getName());
        
        try {
            // Create sample CSV data
            String csvData = "Campaign Name,Ad Name,Impressions,Clicks,CTR,CPC,Spend\n" +
                           "Sample Campaign 1,Sample Ad 1,1000,50,5.0%,$0.50,$25.00\n" +
                           "Sample Campaign 2,Sample Ad 2,2000,100,5.0%,$0.45,$45.00\n";
            
            byte[] csvBytes = csvData.getBytes();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "ads-data-" + LocalDate.now() + ".csv");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
                
        } catch (Exception e) {
            log.error("Error exporting data for user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}