package com.somagochi.pochakfarm.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshRequest(
    @Schema(description = "재발급에 사용할 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy")
        String refreshToken) {}
