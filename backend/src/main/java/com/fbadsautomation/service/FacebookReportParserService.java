package com.fbadsautomation.service;

import com.fbadsautomation.dto.ImportReportResponse;
import com.fbadsautomation.dto.MatchedReportRow;
import com.fbadsautomation.dto.UnmatchedReportRow;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing Facebook Ads Manager reports (CSV and Excel)
 *
 * Security:
 * - File size validation (max 10MB)
 * - File type whitelist (.csv, .xlsx)
 * - CSV injection prevention
 * - User isolation enforcement
 *
 * Performance:
 * - Streaming parser (doesn't load entire file into memory)
 * - Efficient fuzzy matching (Levenshtein distance)
 * - Cached user ads for matching
 *
 * Maintainability:
 * - Clear separation of concerns (CSV vs Excel vs matching logic)
 * - Comprehensive error handling
 * - Detailed logging
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookReportParserService {

    private final AdRepository adRepository;

    // Security: Max file size 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Performance: Fuzzy match threshold
    private static final double FUZZY_MATCH_THRESHOLD = 0.85;

    // Security: CSV injection detection patterns
    private static final Pattern CSV_INJECTION_PATTERN = Pattern.compile("^[=+\\-@]");

    // Pattern to extract ad ID from ad name (if we tagged it during export)
    private static final Pattern AD_ID_PATTERN = Pattern.compile("\\[ID:(\\d+)\\]");

    // Required columns for Facebook report
    private static final String[] REQUIRED_COLUMNS = {
        "Ad name", "Impressions", "Clicks", "Spend"
    };

    // Column name variations Facebook might use
    private static final Map<String, String[]> COLUMN_ALIASES = new HashMap<String, String[]>() {{
        put("adName", new String[]{"Ad name", "Ad Name", "ad_name", "Ad"});
        put("impressions", new String[]{"Impressions", "impressions", "Impr."});
        put("clicks", new String[]{"Clicks", "clicks", "Link clicks"});
        put("spend", new String[]{"Spend", "Amount spent", "spend", "Cost"});
        put("ctr", new String[]{"CTR", "CTR (%)", "ctr", "Click-through rate"});
        put("cpc", new String[]{"CPC", "CPC (cost per link click)", "cpc", "Cost per click"});
        put("cpm", new String[]{"CPM", "CPM (cost per 1,000 impressions)", "cpm"});
        put("conversions", new String[]{"Conversions", "conversions", "Results"});
        put("conversionRate", new String[]{"Conversion rate", "conversion_rate", "Result rate"});
        put("reportDate", new String[]{"Reporting starts", "Date", "report_date", "Day"});
    }};

    /**
     * Parse CSV report from Facebook
     *
     * @param file Uploaded CSV file
     * @param userId User ID for ad matching
     * @return Import response with matched/unmatched rows
     */
    public ImportReportResponse parseCsvReport(MultipartFile file, Long userId) throws IOException {
        log.info("Parsing CSV report for user: {}, file: {}", userId, file.getOriginalFilename());

        // Validation
        validateFile(file);

        List<MatchedReportRow> matched = new ArrayList<>();
        List<UnmatchedReportRow> unmatched = new ArrayList<>();

        // Load user's ads once (performance optimization)
        List<Ad> userAds = adRepository.findByUserId(userId);
        log.debug("Loaded {} ads for user {}", userAds.size(), userId);

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreHeaderCase()
                 .withTrim())) {

            // Validate columns exist
            validateColumns(csvParser.getHeaderMap());

            int rowNum = 1;
            for (CSVRecord record : csvParser) {
                rowNum++;

                try {
                    String adName = getColumn(record, "adName");

                    if (!StringUtils.hasText(adName)) {
                        log.warn("Row {}: Empty ad name, skipping", rowNum);
                        continue;
                    }

                    // Security: Check for CSV injection
                    if (isCsvInjection(adName)) {
                        log.warn("Row {}: Potential CSV injection detected in ad name: {}", rowNum, adName);
                        unmatched.add(UnmatchedReportRow.builder()
                            .rowNumber(rowNum)
                            .facebookAdName(sanitize(adName))
                            .reason("Security: Suspicious characters detected")
                            .build());
                        continue;
                    }

                    // Try to match ad
                    MatchResult matchResult = findMatchingAd(adName, userAds);

                    if (matchResult.ad != null) {
                        MatchedReportRow row = buildMatchedRow(record, matchResult, rowNum);
                        matched.add(row);
                    } else {
                        UnmatchedReportRow row = buildUnmatchedRow(rowNum, record, adName, matchResult.reason);
                        unmatched.add(row);
                    }
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", rowNum, e.getMessage(), e);
                    unmatched.add(UnmatchedReportRow.builder()
                        .rowNumber(rowNum)
                        .reason("Parse error: " + e.getMessage())
                        .build());
                }
            }
        }

        log.info("CSV parsing complete: {} matched, {} unmatched", matched.size(), unmatched.size());

        return ImportReportResponse.builder()
            .totalRows(matched.size() + unmatched.size())
            .matchedRows(matched.size())
            .unmatchedRows(unmatched.size())
            .matchedReports(matched)
            .unmatchedReports(unmatched)
            .hasWarnings(unmatched.size() > 0)
            .message(String.format("Parsed %d rows: %d matched, %d unmatched",
                matched.size() + unmatched.size(), matched.size(), unmatched.size()))
            .build();
    }

    /**
     * Parse Excel report from Facebook
     *
     * @param file Uploaded Excel file
     * @param userId User ID for ad matching
     * @return Import response with matched/unmatched rows
     */
    public ImportReportResponse parseExcelReport(MultipartFile file, Long userId) throws IOException {
        log.info("Parsing Excel report for user: {}, file: {}", userId, file.getOriginalFilename());

        // Validation
        validateFile(file);

        List<MatchedReportRow> matched = new ArrayList<>();
        List<UnmatchedReportRow> unmatched = new ArrayList<>();

        // Load user's ads once
        List<Ad> userAds = adRepository.findByUserId(userId);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // Use first sheet

            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Excel file is empty or has no data rows");
            }

            // Read header row
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = buildColumnMap(headerRow);

            // Validate required columns
            validateExcelColumns(columnMap);

            // Process data rows
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                try {
                    String adName = getCellValue(row, columnMap.get("adName"));

                    if (!StringUtils.hasText(adName)) {
                        continue;
                    }

                    // Security: CSV injection check
                    if (isCsvInjection(adName)) {
                        unmatched.add(UnmatchedReportRow.builder()
                            .rowNumber(rowNum + 1)
                            .facebookAdName(sanitize(adName))
                            .reason("Security: Suspicious characters detected")
                            .build());
                        continue;
                    }

                    // Match ad
                    MatchResult matchResult = findMatchingAd(adName, userAds);

                    if (matchResult.ad != null) {
                        MatchedReportRow matchedRow = buildMatchedRowFromExcel(row, columnMap, matchResult, rowNum + 1);
                        matched.add(matchedRow);
                    } else {
                        UnmatchedReportRow unmatchedRow = buildUnmatchedRowFromExcel(rowNum + 1, row, columnMap, adName, matchResult.reason);
                        unmatched.add(unmatchedRow);
                    }
                } catch (Exception e) {
                    log.error("Error parsing Excel row {}: {}", rowNum + 1, e.getMessage(), e);
                    unmatched.add(UnmatchedReportRow.builder()
                        .rowNumber(rowNum + 1)
                        .reason("Parse error: " + e.getMessage())
                        .build());
                }
            }
        }

        log.info("Excel parsing complete: {} matched, {} unmatched", matched.size(), unmatched.size());

        return ImportReportResponse.builder()
            .totalRows(matched.size() + unmatched.size())
            .matchedRows(matched.size())
            .unmatchedRows(unmatched.size())
            .matchedReports(matched)
            .unmatchedReports(unmatched)
            .hasWarnings(unmatched.size() > 0)
            .message(String.format("Parsed %d rows: %d matched, %d unmatched",
                matched.size() + unmatched.size(), matched.size(), unmatched.size()))
            .build();
    }

    /**
     * Find matching ad using multiple strategies
     *
     * Priority: 1) Exact match, 2) ID extraction, 3) Fuzzy match
     */
    private MatchResult findMatchingAd(String facebookAdName, List<Ad> userAds) {
        // Strategy 1: Exact match
        Optional<Ad> exactMatch = userAds.stream()
            .filter(ad -> facebookAdName.equals(ad.getName()))
            .findFirst();

        if (exactMatch.isPresent()) {
            return new MatchResult(exactMatch.get(), 1.0, "EXACT", "Exact name match");
        }

        // Strategy 2: ID extraction (if we tagged during export)
        Matcher matcher = AD_ID_PATTERN.matcher(facebookAdName);
        if (matcher.find()) {
            Long adId = Long.parseLong(matcher.group(1));
            Optional<Ad> idMatch = userAds.stream()
                .filter(ad -> ad.getId().equals(adId))
                .findFirst();

            if (idMatch.isPresent()) {
                return new MatchResult(idMatch.get(), 1.0, "ID_EXTRACTION", "Matched by embedded ID");
            }
        }

        // Strategy 3: Fuzzy match
        LevenshteinDistance distance = new LevenshteinDistance();
        Optional<MatchResult> fuzzyMatch = userAds.stream()
            .map(ad -> {
                int dist = distance.apply(facebookAdName, ad.getName());
                int maxLen = Math.max(facebookAdName.length(), ad.getName().length());
                double similarity = 1.0 - ((double) dist / maxLen);
                return new MatchResult(ad, similarity, "FUZZY", "Fuzzy match (Levenshtein)");
            })
            .filter(result -> result.confidence >= FUZZY_MATCH_THRESHOLD)
            .max(Comparator.comparingDouble(r -> r.confidence));

        if (fuzzyMatch.isPresent()) {
            return fuzzyMatch.get();
        }

        // No match found
        return new MatchResult(null, 0.0, "NONE", "No matching ad found in your account");
    }

    /**
     * Build matched row from CSV record
     */
    private MatchedReportRow buildMatchedRow(CSVRecord record, MatchResult matchResult, int rowNum) {
        Ad ad = matchResult.ad;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("impressions", parseLong(getColumn(record, "impressions")));
        metrics.put("clicks", parseLong(getColumn(record, "clicks")));
        metrics.put("spend", parseDouble(getColumn(record, "spend")));
        metrics.put("ctr", parseDouble(getColumn(record, "ctr")));
        metrics.put("cpc", parseDouble(getColumn(record, "cpc")));
        metrics.put("cpm", parseDouble(getColumn(record, "cpm")));
        metrics.put("conversions", parseLong(getColumn(record, "conversions")));
        metrics.put("conversionRate", parseDouble(getColumn(record, "conversionRate")));
        metrics.put("reportDate", parseDate(getColumn(record, "reportDate")));
        metrics.put("rowNumber", rowNum);

        return MatchedReportRow.builder()
            .facebookAdName(getColumn(record, "adName"))
            .adId(ad.getId())
            .adName(ad.getName())
            .campaignId(ad.getCampaign() != null ? ad.getCampaign().getId() : null)
            .campaignName(ad.getCampaign() != null ? ad.getCampaign().getName() : null)
            .matchConfidence(matchResult.confidence)
            .matchType(matchResult.matchType)
            .metrics(metrics)
            .build();
    }

    /**
     * Build matched row from Excel row
     */
    private MatchedReportRow buildMatchedRowFromExcel(Row row, Map<String, Integer> columnMap,
                                                       MatchResult matchResult, int rowNum) {
        Ad ad = matchResult.ad;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("impressions", parseLongFromCell(row, columnMap.get("impressions")));
        metrics.put("clicks", parseLongFromCell(row, columnMap.get("clicks")));
        metrics.put("spend", parseDoubleFromCell(row, columnMap.get("spend")));
        metrics.put("ctr", parseDoubleFromCell(row, columnMap.get("ctr")));
        metrics.put("cpc", parseDoubleFromCell(row, columnMap.get("cpc")));
        metrics.put("cpm", parseDoubleFromCell(row, columnMap.get("cpm")));
        metrics.put("conversions", parseLongFromCell(row, columnMap.get("conversions")));
        metrics.put("conversionRate", parseDoubleFromCell(row, columnMap.get("conversionRate")));
        metrics.put("reportDate", parseDateFromCell(row, columnMap.get("reportDate")));
        metrics.put("rowNumber", rowNum);

        return MatchedReportRow.builder()
            .facebookAdName(getCellValue(row, columnMap.get("adName")))
            .adId(ad.getId())
            .adName(ad.getName())
            .campaignId(ad.getCampaign() != null ? ad.getCampaign().getId() : null)
            .campaignName(ad.getCampaign() != null ? ad.getCampaign().getName() : null)
            .matchConfidence(matchResult.confidence)
            .matchType(matchResult.matchType)
            .metrics(metrics)
            .build();
    }

    /**
     * Build unmatched row from CSV record
     */
    private UnmatchedReportRow buildUnmatchedRow(int rowNum, CSVRecord record, String adName, String reason) {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("adName", adName);
        rawData.put("impressions", getColumn(record, "impressions"));
        rawData.put("clicks", getColumn(record, "clicks"));
        rawData.put("spend", getColumn(record, "spend"));

        return UnmatchedReportRow.builder()
            .rowNumber(rowNum)
            .facebookAdName(adName)
            .rawData(rawData)
            .reason(reason)
            .build();
    }

    /**
     * Build unmatched row from Excel row
     */
    private UnmatchedReportRow buildUnmatchedRowFromExcel(int rowNum, Row row,
                                                           Map<String, Integer> columnMap,
                                                           String adName, String reason) {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("adName", adName);
        rawData.put("impressions", getCellValue(row, columnMap.get("impressions")));
        rawData.put("clicks", getCellValue(row, columnMap.get("clicks")));
        rawData.put("spend", getCellValue(row, columnMap.get("spend")));

        return UnmatchedReportRow.builder()
            .rowNumber(rowNum)
            .facebookAdName(adName)
            .rawData(rawData)
            .reason(reason)
            .build();
    }

    /**
     * Get column value from CSV record using aliases
     */
    private String getColumn(CSVRecord record, String columnType) {
        for (String alias : COLUMN_ALIASES.get(columnType)) {
            if (record.isMapped(alias)) {
                return record.get(alias);
            }
        }
        return null;
    }

    /**
     * Build column map for Excel
     */
    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;

            String header = getCellValue(cell).trim();

            // Match against aliases
            for (Map.Entry<String, String[]> entry : COLUMN_ALIASES.entrySet()) {
                for (String alias : entry.getValue()) {
                    if (header.equalsIgnoreCase(alias)) {
                        columnMap.put(entry.getKey(), i);
                        break;
                    }
                }
            }
        }

        return columnMap;
    }

    /**
     * Get cell value as string
     */
    private String getCellValue(Row row, Integer columnIndex) {
        if (columnIndex == null || columnIndex < 0) return null;

        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;

        return getCellValue(cell);
    }

    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Parse long from cell
     */
    private Long parseLongFromCell(Row row, Integer columnIndex) {
        String value = getCellValue(row, columnIndex);
        return parseLong(value);
    }

    /**
     * Parse double from cell
     */
    private Double parseDoubleFromCell(Row row, Integer columnIndex) {
        String value = getCellValue(row, columnIndex);
        return parseDouble(value);
    }

    /**
     * Parse date from cell
     */
    private LocalDate parseDateFromCell(Row row, Integer columnIndex) {
        String value = getCellValue(row, columnIndex);
        return parseDate(value);
    }

    /**
     * Parse long from string (handles formatting)
     */
    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) return null;

        try {
            // Remove commas and other formatting
            String cleaned = value.replaceAll("[,\\s]", "");
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse double from string (handles currency, percentages)
     */
    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) return null;

        try {
            // Remove currency symbols, commas, percent signs
            String cleaned = value.replaceAll("[$,€£¥%\\s]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse date from string (handles multiple formats)
     */
    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) return LocalDate.now();

        List<DateTimeFormatter> formatters = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        log.warn("Could not parse date: {}, using today's date", value);
        return LocalDate.now();
    }

    /**
     * Validate file size and type
     */
    private void validateFile(MultipartFile file) {
        // Check null
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        // Check size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                String.format("File size exceeds limit of %d MB", MAX_FILE_SIZE / (1024 * 1024)));
        }

        // Check type
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (contentType == null || filename == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file");
        }

        boolean isValidType = contentType.equals("text/csv") ||
            contentType.contains("spreadsheet") ||
            filename.toLowerCase().endsWith(".csv") ||
            filename.toLowerCase().endsWith(".xlsx");

        if (!isValidType) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Invalid file type. Only CSV and Excel (.xlsx) files are supported");
        }
    }

    /**
     * Validate required columns exist
     */
    private void validateColumns(Map<String, Integer> headers) {
        for (String required : REQUIRED_COLUMNS) {
            boolean found = headers.keySet().stream()
                .anyMatch(h -> h.equalsIgnoreCase(required));

            if (!found) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Missing required column: " + required +
                        ". Please ensure you're uploading a valid Facebook Ads Manager report.");
            }
        }
    }

    /**
     * Validate Excel columns
     */
    private void validateExcelColumns(Map<String, Integer> columnMap) {
        if (!columnMap.containsKey("adName")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing required column: Ad name");
        }
        if (!columnMap.containsKey("impressions")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing required column: Impressions");
        }
        if (!columnMap.containsKey("clicks")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing required column: Clicks");
        }
    }

    /**
     * Check for CSV injection patterns
     */
    private boolean isCsvInjection(String value) {
        if (!StringUtils.hasText(value)) return false;
        return CSV_INJECTION_PATTERN.matcher(value.trim()).find();
    }

    /**
     * Sanitize potentially dangerous input
     */
    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) return value;

        // Remove leading special characters
        String sanitized = value.replaceAll("^[=+\\-@]+", "");

        // Escape quotes
        sanitized = sanitized.replace("\"", "\"\"");

        return sanitized;
    }

    /**
     * Inner class to hold match result
     */
    private static class MatchResult {
        Ad ad;
        double confidence;
        String matchType;
        String reason;

        MatchResult(Ad ad, double confidence, String matchType, String reason) {
            this.ad = ad;
            this.confidence = confidence;
            this.matchType = matchType;
            this.reason = reason;
        }
    }
}
