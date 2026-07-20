package com.somagochi.pochakfarm.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmRequest(
    @Schema(
            description = "presign 단계에서 발급받은 객체 key",
            example = "images/profile/1/9f8b0c2e-1a2b-4c3d-8e9f-0a1b2c3d4e5f.png")
        String key) {}
