package com.fbadsautomation.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Combined response for Facebook export operations.
 * Contains the generated export file (CSV/Excel), normalized payloads, and auto-upload metadata.
 */
@Value
@Builder
public class FacebookExportResponse {
    String filename;
    String format;
    byte[] fileContent;
    List<FacebookAdPayload> payloads;
    FacebookAutoExportResponse autoUpload;
}
