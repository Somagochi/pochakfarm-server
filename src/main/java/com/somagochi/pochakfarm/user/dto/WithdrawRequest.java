package com.somagochi.pochakfarm.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record WithdrawRequest(
    @Schema(description = "함께 무효화할 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy")
        String refreshToken) {}
