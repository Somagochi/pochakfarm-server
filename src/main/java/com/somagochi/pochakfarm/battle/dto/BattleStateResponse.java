package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "대전 진행 상태. 재접속 시 이 응답만으로 대전 화면을 복원한다")
public record BattleStateResponse(
    @Schema(description = "대전 ID", example = "1") Long battleId,
    @Schema(description = "관장 ID", example = "1") Long gymLeaderId,
    @Schema(description = "대전 상태", example = "IN_PROGRESS") BattleStatus status,
    @Schema(description = "대전 결과. 진행 중이면 null", example = "WIN") BattleResult result,
    @Schema(description = "현재 승부 바 위치", example = "4") int barPosition,
    @Schema(description = "승부 바 최솟값", example = "-15") int minBarPosition,
    @Schema(description = "승부 바 최댓값", example = "15") int maxBarPosition,
    @Schema(description = "지금까지 처리된 행동 횟수(0~9)", example = "3") int completedActionCount,
    @Schema(description = "총 행동 횟수", example = "9") int totalActionCount,
    @Schema(description = "현재 출전 순번(1~3)", example = "2") int currentEntryOrder,
    @Schema(description = "다음에 보낼 행동 순번. 더 진행할 행동이 없으면 null", example = "4") Integer nextActionSeq,
    @Schema(description = "다음 행동의 서버 기준 선택 마감 시각. 다음 행동이 없으면 null") Instant nextSelectionExpiresAt,
    @Schema(description = "현재 출전 중인 유저 동물") BattleEntryResponse userEntry,
    @Schema(description = "현재 출전 중인 관장 동물") BattleEntryResponse npcEntry,
    @Schema(description = "지금까지 발생한 중계 이벤트 전체")
        List<BattleBroadcastEventResponse> broadcastEvents) {}
