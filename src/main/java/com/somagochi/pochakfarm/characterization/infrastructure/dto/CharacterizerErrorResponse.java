package com.somagochi.pochakfarm.characterization.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CharacterizerErrorResponse(
    String status, @JsonProperty("error_code") String errorCode, String message) {}
