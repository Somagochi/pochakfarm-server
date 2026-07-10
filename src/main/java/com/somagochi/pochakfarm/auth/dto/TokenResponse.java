package com.somagochi.pochakfarm.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
    @Schema(description = "API 인증에 사용할 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.aaaaa.bbbbb")
        String accessToken,
    @Schema(description = "액세스 토큰 재발급에 사용할 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.ccccc.ddddd")
        String refreshToken) {}
