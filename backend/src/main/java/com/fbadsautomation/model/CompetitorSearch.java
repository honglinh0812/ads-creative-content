package com.fbadsautomation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity representing a competitor brand search history entry
 *
 * Stores user's searches for competitor ads to provide:
 * - Search history tracking
 * - Autocomplete suggestions
 * - Usage analytics
 *
 * @author AI Panel - Senior Engineers
 * @version 1.0
 * @security User-scoped data, no cross-user access
 */
@Entity
@Table(name = "competitor_searches",
       indexes = {
           @Index(name = "idx_competitor_search_user", columnList = "user_id"),
           @Index(name = "idx_competitor_search_date", columnList = "search_date DESC"),
           @Index(name = "idx_competitor_search_brand_user", columnList = "brand_name, user_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitorSearch {

    /**
     * Primary key - auto-generated
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Brand name searched
     * Security: Sanitized before storage
     * Performance: Indexed for autocomplete queries
     */
    @Column(name = "brand_name", nullable = false, length = 100)
    @NotBlank(message = "Brand name is required")
    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    private String brandName;

    /**
     * Industry category (optional)
     * Also used to store region code for regional searches
     */
    @Column(name = "industry", length = 50)
    @Size(max = 50, message = "Industry must not exceed 50 characters")
    private String industry;

    /**
     * Region code where search was performed
     * ISO 3166-1 alpha-2 codes (US, UK, VN, etc.)
     */
    @Column(name = "region", length = 10)
    @Size(max = 10, message = "Region code must not exceed 10 characters")
    private String region;

    /**
     * User who performed the search
     * Security: Enforces ownership, enables user-scoped queries
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * Timestamp of search
     * Performance: Indexed for sorting and date range queries
     */
    @Column(name = "search_date", nullable = false)
    @NotNull(message = "Search date is required")
    private LocalDateTime searchDate;

    /**
     * Number of results returned (optional, for analytics)
     */
    @Column(name = "result_count")
    private Integer resultCount;

    /**
     * Search query type (BRAND, URL, etc.)
     */
    @Column(name = "search_type", length = 20)
    @Size(max = 20, message = "Search type must not exceed 20 characters")
    private String searchType;

    /**
     * Whether search was successful
     */
    @Column(name = "success")
    private Boolean success;

    /**
     * Pre-persist callback to set search date if not already set
     */
    @PrePersist
    protected void onCreate() {
        if (searchDate == null) {
            searchDate = LocalDateTime.now();
        }
        if (searchType == null) {
            searchType = "BRAND";
        }
        if (success == null) {
            success = true;
        }
    }
}
