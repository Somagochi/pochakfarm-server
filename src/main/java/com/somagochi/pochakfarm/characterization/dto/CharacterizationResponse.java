package com.somagochi.pochakfarm.characterization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterizationResponse(
    @Schema(description = "변환 처리 결과 상태", example = "success") String status,
    @Schema(description = "실제 변환을 수행한 내부 provider", example = "codex_exec") String provider,
    @Schema(
            description = "fallback provider가 사용된 경우 원래 provider",
            nullable = true,
            example = "primary")
        String fallbackFrom,
    @Schema(description = "요청에서 전달한 반려동물 이름", example = "솜구름") String animalName,
    @Schema(description = "카드 타입", example = "하늘") String cardType,
    @Schema(description = "카드 파워", example = "82") Integer power,
    @Schema(description = "첫 번째 스킬명", example = "몽실몽실") String skill1Name,
    @Schema(description = "첫 번째 스킬 설명", example = "몽실한 솜털로 모두의 마음을 사르르 녹여요.")
        String skill1Description,
    @Schema(description = "두 번째 스킬명", example = "해맑은 미소") String skill2Name,
    @Schema(description = "두 번째 스킬 설명", example = "기분 좋은 에너지를 나누며 주변을 밝게 해요.")
        String skill2Description,
    @Schema(description = "카드 번호", example = "No.001") String cardNo,
    @Schema(
            description = "S3에 저장된 결과 이미지 접근 URL",
            example = "https://cdn.example.com/public/characterization-result/result.png")
        String resultImageUrl,
    @Schema(description = "Python 변환 서버가 보고한 처리 시간(ms)", example = "12345") Integer elapsedMs) {}
