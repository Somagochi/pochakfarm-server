package com.somagochi.pochakfarm.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SocialLoginRequest(
    @Schema(
            description = "소셜 provider (대소문자 무관)",
            allowableValues = {"kakao", "naver", "apple"},
            example = "kakao",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String provider,
    @Schema(
            description =
                "App SDK로 발급받은 provider 토큰. 카카오/네이버는 access token, 애플은 id token(JWT). "
                    + "서버가 이 토큰으로 provider userinfo 조회/검증 후 로그인한다.",
            example = "kaAbc123...(provider access token)",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String token) {}
