package com.somagochi.pochakfarm.characterization.dto;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterizationResponse(
    @Schema(description = "생성된 캐릭터라이징 ID", example = "1") Long characterizationId,
    @Schema(description = "캐릭터라이징 작업 상태", example = "SUCCEEDED") CharacterizationStatus status,
    @Schema(
            description = "S3에 저장된 결과 이미지 접근 URL",
            example = "https://cdn.example.com/public/characterization-result/result.png")
        String resultImageUrl,
    @Schema(description = "카드 뒷면 리소스 매칭용 카드 타입", example = "GROUND") CardType cardType,
    @Schema(description = "실패 사유 코드. 실패 상태가 아니면 null", example = "CHARACTERIZATION_FAILED")
        String failureReason) {}
