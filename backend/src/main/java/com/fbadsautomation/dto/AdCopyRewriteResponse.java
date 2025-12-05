package com.fbadsautomation.dto;

public class AdCopyRewriteResponse {
    private String section;
    private String rewrittenText;

    public AdCopyRewriteResponse() {}

    public AdCopyRewriteResponse(String section, String rewrittenText) {
        this.section = section;
        this.rewrittenText = rewrittenText;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getRewrittenText() {
        return rewrittenText;
    }

    public void setRewrittenText(String rewrittenText) {
        this.rewrittenText = rewrittenText;
    }
}
