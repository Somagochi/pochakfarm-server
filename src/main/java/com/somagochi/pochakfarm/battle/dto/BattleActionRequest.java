package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스킬 선택 행동 요청")
public record BattleActionRequest(
    @Schema(description = "행동 순번(1~9). 같은 순번의 재요청은 최초 판정 결과를 그대로 돌려준다", example = "1")
        Integer actionSeq,
    @Schema(
            description = "선택한 스킬. 클라이언트에서 시간 초과 또는 미선택으로 처리한 경우 null 로 보내며 유저 획득 승부 포인트는 0으로 처리한다",
            example = "SEA_WAVE_DASH")
        CardSkill skill) {}
