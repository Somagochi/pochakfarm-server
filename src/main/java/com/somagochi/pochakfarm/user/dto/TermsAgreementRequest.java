package com.somagochi.pochakfarm.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TermsAgreementRequest(
    @Schema(description = "만 14세 이상 필수 동의", example = "true") Boolean ageRequirementAgreed,
    @Schema(description = "이용약관 필수 동의", example = "true") Boolean termsOfServiceAgreed,
    @Schema(description = "개인정보 수집 및 이용 필수 동의", example = "true") Boolean privacyPolicyAgreed,
    @Schema(description = "서비스 품질 향상 선택 동의", example = "false") Boolean serviceQualityAgreed,
    @Schema(description = "이벤트 및 혜택 알림 수신 선택 동의", example = "true") Boolean marketingAgreed) {}
