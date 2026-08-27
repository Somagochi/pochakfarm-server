package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "대전 시작 요청")
public record BattleStartRequest(
    @Schema(description = "도전할 관장 ID", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        Long gymLeaderId,
    @Schema(
            description = "중복 생성 방지를 위한 클라이언트 요청 식별자",
            example = "b3f1c2a0-5d6e-4f7a-8b9c-0d1e2f3a4b5c",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String clientRequestId,
    @Schema(description = "출전 동물 3마리와 출전 순서", requiredMode = Schema.RequiredMode.REQUIRED)
        List<BattleEntryRequest> entries) {}
