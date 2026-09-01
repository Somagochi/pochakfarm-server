package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "최종 승부 탭 결과")
public record BattleFinalRoundResultRequest(
    @Schema(description = "3초간 한 손가락으로 입력한 탭 횟수", example = "20") Integer tapCount) {}
