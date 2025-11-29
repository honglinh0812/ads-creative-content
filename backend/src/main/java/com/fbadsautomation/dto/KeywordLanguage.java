package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordLanguage implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("language_name")
    private String languageName;

    @JsonProperty("language_code")
    private String languageCode;
}
