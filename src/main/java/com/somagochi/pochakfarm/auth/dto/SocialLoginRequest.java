package com.somagochi.pochakfarm.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SocialLoginRequest(
    @Schema(
            description = "소셜 로그인 제공자",
            example = "KAKAO",
            allowableValues = {"KAKAO", "NAVER", "APPLE"})
        String provider,
    @Schema(
            description = "소셜 provider에서 발급받은 인증 토큰 (액세스 토큰 또는 ID 토큰)",
            example = "ya29.a0AfH6SMBxxxxx")
        String token) {}
