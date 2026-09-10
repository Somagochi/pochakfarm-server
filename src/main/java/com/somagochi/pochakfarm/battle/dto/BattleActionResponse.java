package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "스킬 선택 행동 판정 결과")
public record BattleActionResponse(
    @Schema(description = "대전 ID", example = "1") Long battleId,
    @Schema(description = "행동 순번(1~9)", example = "3") int actionSeq,
    @Schema(description = "출전 순번(1~3)", example = "1") int entryOrder,
    @Schema(description = "해당 동물의 몇 번째 행동인지(1~3)", example = "3") int actionNoInEntry,
    @Schema(description = "유저 스킬 판정 결과") BattleSkillOutcomeResponse user,
    @Schema(description = "NPC 스킬 판정 결과") BattleSkillOutcomeResponse npc,
    @Schema(description = "유저 획득 포인트 - NPC 획득 포인트로 계산한 순승부 포인트", example = "1") int netPoint,
    @Schema(description = "행동 처리 후 승부 바 위치. 동물 교체가 함께 일어났으면 교체 시 반영된 우위까지 포함한다", example = "4")
        int barPosition,
    @Schema(description = "승부 바 최솟값", example = "-15") int minBarPosition,
    @Schema(description = "승부 바 최댓값", example = "15") int maxBarPosition,
    @Schema(description = "대전 상태", example = "IN_PROGRESS") BattleStatus battleStatus,
    @Schema(description = "대전 결과. 진행 중이면 null", example = "WIN") BattleResult battleResult,
    @Schema(description = "다음 행동 순번. 더 진행할 행동이 없으면 null", example = "4") Integer nextActionSeq,
    @Schema(description = "최종 승부 진행 상태") BattleFinalRoundStateResponse finalRound,
    @Schema(description = "대전이 종료되었을 때의 보상·성장 결과. 진행 중이면 null") BattleRewardResponse reward,
    @Schema(description = "이번 행동에서 발생한 중계 이벤트 목록")
        List<BattleBroadcastEventResponse> broadcastEvents) {}
