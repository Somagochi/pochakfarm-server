package com.somagochi.pochakfarm.capture.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CaptureStartRequest(
    @Schema(
            description = "앱이 촬영 1건마다 생성하는 UUID. 네트워크 재시도 시 동일한 값을 사용한다.",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String clientRequestId,
    @Schema(
            description = "업로드할 원본 이미지 MIME 타입",
            example = "image/jpeg",
            allowableValues = {"image/jpeg", "image/png", "image/webp"})
        String contentType,
    @Schema(description = "카드 이미지에 고정할 동물 이름. 1~6글자", example = "두부") String animalName,
    @Schema(description = "무료 횟수 소진 시 200코인 결제 동의 여부", example = "false")
        Boolean allowCoinPayment) {

  public CaptureStartRequest {
    allowCoinPayment = Boolean.TRUE.equals(allowCoinPayment);
  }

  public CaptureStartRequest(String clientRequestId, String contentType, String animalName) {
    this(clientRequestId, contentType, animalName, false);
  }
}
