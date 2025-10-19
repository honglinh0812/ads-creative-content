package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingKeyword implements Serializable {

    private static final long serialVersionUID = 1L;

    private String keyword;
    private Integer growth; // Growth percentage
    private String region;
    private Long searchVolume;
    private String category;

    public TrendingKeyword(String keyword, Integer growth) {
        this.keyword = keyword;
        this.growth = growth;
    }
}
