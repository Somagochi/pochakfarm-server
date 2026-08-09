package com.somagochi.pochakfarm.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TermsAgreementUpdateRequest(
    @Schema(
            description = "이벤트 및 혜택 알림 수신 선택 동의 여부",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean marketingAgreed) {}
