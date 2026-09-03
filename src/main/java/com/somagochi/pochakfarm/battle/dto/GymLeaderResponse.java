package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 관장 목록 항목. 상세 정보는 관장 상세 API 로 조회한다")
public record GymLeaderResponse(
    @Schema(description = "관장 ID", example = "4") Long gymLeaderId,
    @Schema(description = "관장 이름", example = "노바") String name,
    @Schema(description = "도전 순서", example = "4") int challengeOrder,
    @Schema(description = "관장 썸네일 이미지 URL. 에셋 미확정이면 null") String thumbnailUrl,
    @Schema(description = "클리어 여부. 해당 관장의 뱃지 보유 여부와 같다", example = "false") boolean cleared,
    @Schema(description = "해금 여부. 해금 조건 상세는 관장 상세 API 로 조회한다", example = "false")
        boolean unlocked) {}
