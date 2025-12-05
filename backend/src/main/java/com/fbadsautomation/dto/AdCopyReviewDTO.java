package com.fbadsautomation.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents AI-powered feedback for ad copy sections
 * (headline, description, primary text). Used by the optimization
 * view to render actionable insights and rewrite suggestions.
 */
public class AdCopyReviewDTO {

    private String personaSummary;
    private double overallScore;
    private String overallVerdict;
    private List<SectionReview> sections = new ArrayList<>();

    public String getPersonaSummary() {
        return personaSummary;
    }

    public void setPersonaSummary(String personaSummary) {
        this.personaSummary = personaSummary;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public String getOverallVerdict() {
        return overallVerdict;
    }

    public void setOverallVerdict(String overallVerdict) {
        this.overallVerdict = overallVerdict;
    }

    public List<SectionReview> getSections() {
        return sections;
    }

    public void setSections(List<SectionReview> sections) {
        this.sections = sections;
    }

    public static class SectionReview {
        private String section;
        private double score;
        private String verdict;
        private List<String> strengths = new ArrayList<>();
        private List<String> improvements = new ArrayList<>();
        private String rewrite;

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getVerdict() {
            return verdict;
        }

        public void setVerdict(String verdict) {
            this.verdict = verdict;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getImprovements() {
            return improvements;
        }

        public void setImprovements(List<String> improvements) {
            this.improvements = improvements;
        }

        public String getRewrite() {
            return rewrite;
        }

        public void setRewrite(String rewrite) {
            this.rewrite = rewrite;
        }
    }
}
