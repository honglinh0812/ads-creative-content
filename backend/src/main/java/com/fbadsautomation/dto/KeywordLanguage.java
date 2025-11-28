package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordLanguage {

    @JsonProperty("language_name")
    private String languageName;

    @JsonProperty("language_code")
    private String languageCode;
}
