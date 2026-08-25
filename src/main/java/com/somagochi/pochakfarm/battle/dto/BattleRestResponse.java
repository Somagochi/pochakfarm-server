package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "출전 동물의 휴식 종료 시각")
public record BattleRestResponse(
    @Schema(description = "동물 ID", example = "31") Long animalId,
    @Schema(description = "휴식 종료 시각. 서버 시각 기준", example = "2026-08-25T06:30:00Z")
        Instant restEndsAt) {}
