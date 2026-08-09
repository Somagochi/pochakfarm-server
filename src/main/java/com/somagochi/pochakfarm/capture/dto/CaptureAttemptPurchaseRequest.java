package com.somagochi.pochakfarm.capture.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CaptureAttemptPurchaseRequest(
    @Schema(
            description = "구매 요청별 UUID. 네트워크 재시도 시 동일한 값을 사용한다.",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String clientRequestId) {}
