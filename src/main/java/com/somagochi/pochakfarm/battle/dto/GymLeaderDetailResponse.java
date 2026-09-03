package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관장 상세. 관장 동물의 스킬·발동 확률·승부 포인트는 포함하지 않는다")
public record GymLeaderDetailResponse(
    @Schema(description = "관장 정보") GymLeaderProfileResponse gymLeader,
    @Schema(description = "관장 동물 3마리. 출전 순서 오름차순") List<GymLeaderAnimalResponse> animals) {}
