package com.somagochi.pochakfarm.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SocialLoginResponse(
    @Schema(description = "발급된 액세스/리프레시 토큰") TokenResponse token,
    @Schema(description = "신규 가입 회원 여부 (true면 이번 로그인에서 회원이 새로 생성됨)", example = "false")
        Boolean isNew,
    @Schema(description = "약관 동의 화면 진입 필요 여부", example = "true") Boolean termsAgreementRequired) {}
