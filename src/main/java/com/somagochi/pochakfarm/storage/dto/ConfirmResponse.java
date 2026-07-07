package com.somagochi.pochakfarm.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmResponse(
    @Schema(
            description = "확정된 객체 key",
            example = "images/profile/1/9f8b0c2e-1a2b-4c3d-8e9f-0a1b2c3d4e5f.png")
        String key,
    @Schema(
            description = "업로드된 이미지의 최종 접근 URL",
            example = "https://cdn.example.com/images/profile/1/9f8b0c2e.png")
        String url) {}
