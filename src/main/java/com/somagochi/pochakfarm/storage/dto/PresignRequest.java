package com.somagochi.pochakfarm.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PresignRequest(
    @Schema(
            description = "업로드할 이미지의 MIME 타입",
            example = "image/png",
            allowableValues = {"image/jpeg", "image/png", "image/webp"})
        String contentType,
    @Schema(
            description = "업로드 용도 분류. 소문자/숫자/하이픈 1~30자",
            example = "profile",
            pattern = "^[a-z0-9-]{1,30}$")
        String purpose) {}
