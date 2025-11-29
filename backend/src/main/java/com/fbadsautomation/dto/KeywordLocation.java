package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("country_name")
    private String countryName;

    @JsonProperty("country_code")
    private String countryCode;
}
