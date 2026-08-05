package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record TermsAgreementResponse(
    @Schema(description = "필수 약관 동의 여부", example = "true") boolean requiredTermsAgreed,
    @Schema(
            description = "필수 약관 동의 일시. 미동의 시 null",
            example = "2026-08-01T10:00:00Z",
            nullable = true)
        Instant requiredTermsAgreedAt,
    @Schema(description = "서비스 품질 향상 선택 동의 여부", example = "false") boolean serviceQualityAgreed,
    @Schema(
            description = "서비스 품질 향상 선택 동의 일시. 미동의 시 null",
            example = "2026-08-01T10:00:00Z",
            nullable = true)
        Instant serviceQualityAgreedAt,
    @Schema(description = "이벤트 및 혜택 알림 수신 선택 동의 여부", example = "true") boolean marketingAgreed,
    @Schema(
            description = "이벤트 및 혜택 알림 수신 선택 동의 일시. 미동의 시 null",
            example = "2026-08-05T10:30:00Z",
            nullable = true)
        Instant marketingAgreedAt) {

  public static TermsAgreementResponse from(User user) {
    return new TermsAgreementResponse(
        user.getRequiredTermsAgreedAt() != null,
        user.getRequiredTermsAgreedAt(),
        user.getServiceQualityAgreedAt() != null,
        user.getServiceQualityAgreedAt(),
        user.getMarketingAgreedAt() != null,
        user.getMarketingAgreedAt());
  }
}
