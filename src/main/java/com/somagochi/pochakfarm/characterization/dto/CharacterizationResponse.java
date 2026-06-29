package com.somagochi.pochakfarm.characterization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterizationResponse(
    @Schema(
            description = "S3에 저장된 AI 생성 이미지 접근 URL",
            example = "https://cdn.example.com/public/characterization-ai/ai.png")
        String aiImageUrl,
    @Schema(
            description = "S3에 저장된 결과 이미지 접근 URL",
            example = "https://cdn.example.com/public/characterization-result/result.png")
        String resultImageUrl) {}
