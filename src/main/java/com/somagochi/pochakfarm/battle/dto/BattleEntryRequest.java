package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대전 출전 편성 항목")
public record BattleEntryRequest(
    @Schema(
            description = "출전할 내 동물 ID",
            example = "31",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long animalId,
    @Schema(description = "출전 순서 (1~3)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer orderNo) {}
