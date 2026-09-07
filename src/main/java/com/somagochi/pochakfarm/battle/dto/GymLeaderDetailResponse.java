package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관장 상세. 관장 동물의 스킬 이름과 전투 유형을 함께 공개한다")
public record GymLeaderDetailResponse(
    @Schema(description = "관장 정보") GymLeaderProfileResponse gymLeader,
    @Schema(description = "관장 동물 3마리. 출전 순서 오름차순") List<GymLeaderAnimalResponse> animals) {}
