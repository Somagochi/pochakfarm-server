package com.somagochi.pochakfarm.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record PresignResponse(
    @Schema(
            description = "이미지를 PUT 방식으로 업로드할 presigned URL",
            example = "https://bucket.s3.amazonaws.com/images/...")
        String uploadUrl,
    @Schema(
            description = "업로드된 객체를 식별하는 key (업로드 확정 시 사용)",
            example = "images/profile/1/9f8b0c2e-1a2b-4c3d-8e9f-0a1b2c3d4e5f.png")
        String key,
    @Schema(description = "presigned URL 만료 시각 (UTC)", example = "2026-07-07T12:34:56Z")
        Instant expiresAt) {}
