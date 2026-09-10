package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 관장 상세 정보")
public record GymLeaderProfileResponse(
    @Schema(description = "관장 ID", example = "4") Long gymLeaderId,
    @Schema(description = "관장 코드", example = "GYM004") String code,
    @Schema(description = "관장 이름", example = "노바") String name,
    @Schema(description = "도전 순서", example = "4") int challengeOrder,
    @Schema(description = "관장 이미지 URL. 에셋 미확정이면 null") String imageUrl,
    @Schema(description = "관장 주력 타입 라벨. 혼합 편성이면 복합이고 미설정이면 null", example = "땅") String leaderType,
    @Schema(description = "난이도 라벨. 미설정이면 null", example = "초보") String difficulty,
    @Schema(description = "관장 소개 문구. 미설정이면 null", example = "모루는 땅 타입만 데리고 나오는 관장이에요")
        String leaderDescription,
    @Schema(description = "공략 팁 문구. 미설정이면 null", example = "모루는 단단한 방어와 꾸준한 공격이 특징이에요")
        String tipDescription,
    @Schema(
            description = "추천 타입 라벨. 관장 주력 타입에 상성 우위인 타입이고 주력 타입이 복합이면 복합이며 주력 타입이 없으면 null",
            example = "하늘")
        String suggestType,
    @Schema(description = "클리어 여부. 해당 관장의 뱃지 보유 여부와 같다", example = "false") boolean cleared,
    @Schema(description = "해금 조건과 충족 여부") GymLeaderUnlockResponse unlock) {}
