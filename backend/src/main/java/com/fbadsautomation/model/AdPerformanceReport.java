package com.fbadsautomation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing performance metrics imported from Facebook Ads Manager reports
 *
 * Security: User isolation enforced via user_id foreign key
 * Performance: Indexed on ad_id, campaign_id, report_date
 * Maintainability: Clear field names, comprehensive validation
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@Entity
@Table(
    name = "ad_performance_reports",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_ad_report_date",
            columnNames = {"ad_id", "report_date", "source"}
        )
    },
    indexes = {
        @Index(name = "idx_reports_ad_id", columnList = "ad_id"),
        @Index(name = "idx_reports_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_reports_user_id", columnList = "user_id"),
        @Index(name = "idx_reports_date", columnList = "report_date"),
        @Index(name = "idx_reports_ad_date", columnList = "ad_id, report_date"),
        @Index(name = "idx_reports_campaign_date", columnList = "campaign_id, report_date"),
        @Index(name = "idx_reports_source", columnList = "source"),
        @Index(name = "idx_reports_user_date", columnList = "user_id, report_date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdPerformanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign Keys
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Performance Metrics
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "ctr")
    private Double ctr; // Click-through rate (percentage)

    @Column(name = "spend")
    private Double spend;

    @Column(name = "cpc")
    private Double cpc; // Cost per click

    @Column(name = "cpm")
    private Double cpm; // Cost per thousand impressions

    @Column(name = "conversions")
    @Builder.Default
    private Long conversions = 0L;

    @Column(name = "conversion_rate")
    private Double conversionRate;

    // Metadata
    @Column(name = "source", length = 50, nullable = false)
    @Builder.Default
    private String source = "FACEBOOK";

    @Column(name = "imported_at", nullable = false)
    @Builder.Default
    private LocalDateTime importedAt = LocalDateTime.now();

    @Column(name = "imported_by", length = 255)
    private String importedBy;

    // Audit Fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Calculate CTR if not provided
     * CTR = (clicks / impressions) * 100
     */
    @PrePersist
    @PreUpdate
    public void calculateDerivedMetrics() {
        // Calculate CTR if not provided
        if (this.ctr == null && this.impressions != null && this.impressions > 0) {
            this.ctr = (this.clicks.doubleValue() / this.impressions.doubleValue()) * 100.0;
        }

        // Calculate CPC if not provided
        if (this.cpc == null && this.spend != null && this.clicks != null && this.clicks > 0) {
            this.cpc = this.spend / this.clicks.doubleValue();
        }

        // Calculate CPM if not provided
        if (this.cpm == null && this.spend != null && this.impressions != null && this.impressions > 0) {
            this.cpm = (this.spend / this.impressions.doubleValue()) * 1000.0;
        }

        // Calculate conversion rate if not provided
        if (this.conversionRate == null && this.conversions != null && this.clicks != null && this.clicks > 0) {
            this.conversionRate = (this.conversions.doubleValue() / this.clicks.doubleValue()) * 100.0;
        }

        // Set imported timestamp if not set
        if (this.importedAt == null) {
            this.importedAt = LocalDateTime.now();
        }
    }

    /**
     * Convenience method to check if this is a Facebook report
     */
    public boolean isFacebookReport() {
        return "FACEBOOK".equalsIgnoreCase(this.source);
    }

    /**
     * Convenience method to check if this is a manual entry
     */
    public boolean isManualEntry() {
        return "MANUAL".equalsIgnoreCase(this.source);
    }
}
