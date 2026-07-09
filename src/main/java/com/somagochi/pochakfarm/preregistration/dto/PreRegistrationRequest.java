package com.somagochi.pochakfarm.preregistration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PreRegistrationRequest(
    @Schema(description = "사전예약자 휴대폰 번호", example = "010-1234-5678") String phoneNumber,
    @Schema(description = "필수 약관 동의 여부 (true 필수)", example = "true") Boolean requiredConsent,
    @Schema(description = "사전예약과 연결할 캐릭터라이징 ID", example = "1") Long characterizationId) {}
