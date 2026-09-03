package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 관장 상세 정보")
public record GymLeaderProfileResponse(
    @Schema(description = "관장 ID", example = "4") Long gymLeaderId,
    @Schema(description = "관장 코드", example = "GYM004") String code,
    @Schema(description = "관장 이름", example = "노바") String name,
    @Schema(description = "도전 순서", example = "4") int challengeOrder,
    @Schema(description = "관장 이미지 URL. 에셋 미확정이면 null") String imageUrl,
    @Schema(description = "승리 시 지급되는 뱃지 코드", example = "BDG009") String badgeCode,
    @Schema(description = "클리어 여부. 해당 관장의 뱃지 보유 여부와 같다", example = "false") boolean cleared,
    @Schema(description = "해금 조건과 충족 여부") GymLeaderUnlockResponse unlock) {}
