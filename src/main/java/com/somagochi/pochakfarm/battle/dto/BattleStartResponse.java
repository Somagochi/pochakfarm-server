package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "대전 시작 결과")
public record BattleStartResponse(
    @Schema(description = "대전 ID", example = "101") Long battleId,
    @Schema(description = "관장 ID", example = "4") Long gymLeaderId,
    @Schema(description = "현재 승부 바 위치. 중앙에서 시작한다", example = "0") int barPosition,
    @Schema(description = "승부 바 최솟값", example = "-15") int minBarPosition,
    @Schema(description = "승부 바 최댓값", example = "15") int maxBarPosition,
    @Schema(description = "1번 출전 내 동물") BattleUserEntryResponse userEntry,
    @Schema(description = "1번 출전 관장 동물") BattleNpcEntryResponse npcEntry,
    @Schema(description = "출전 동물 3마리의 휴식 종료 시각") List<BattleRestResponse> rests) {}
