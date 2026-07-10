package com.somagochi.pochakfarm.characterization.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HttpCharacterizerErrorResponse(
    String status, @JsonProperty("error_code") String errorCode, String message) {}
