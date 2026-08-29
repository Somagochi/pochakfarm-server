package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.BattleBroadcastEvent;
import com.somagochi.pochakfarm.battle.domain.BattleEventCode;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "중계 이벤트. 문구 렌더링은 클라이언트가 eventCode 별 템플릿에 파라미터를 채워 처리한다")
public record BattleBroadcastEventResponse(
    @Schema(description = "경기 내 중계 이벤트 순번", example = "1") int eventSeq,
    @Schema(description = "이 이벤트를 발생시킨 행동 순번", example = "1") Integer actionSeq,
    @Schema(description = "출전 순번(1~3)", example = "1") Integer entryOrder,
    @Schema(description = "중계 이벤트 코드", example = "SKILL_TRIGGERED") BattleEventCode eventCode,
    @Schema(description = "이벤트 주체 진영. 해당 없으면 null", example = "USER") BattleSide animalSide,
    @Schema(description = "이벤트 대상 스킬. 해당 없으면 null", example = "SEA_WAVE_DASH") CardSkill skill,
    @Schema(description = "이벤트 대상 스킬 이름. 해당 없으면 null", example = "파도 돌진") String skillName,
    @Schema(description = "승부 포인트를 획득한 진영. 해당 없으면 null", example = "USER") BattleSide winnerSide,
    @Schema(description = "획득한 승부 포인트. 해당 없으면 null", example = "2") Integer point) {

  public static BattleBroadcastEventResponse from(BattleBroadcastEvent event) {
    CardSkill skill = event.getParamSkill();
    return new BattleBroadcastEventResponse(
        event.getEventSeq(),
        event.getActionSeq(),
        event.getEntryOrder(),
        event.getEventCode(),
        event.getParamAnimalSide(),
        skill,
        skill == null ? null : skill.displayName(),
        event.getParamWinnerSide(),
        event.getParamPoints());
  }

  public static List<BattleBroadcastEventResponse> from(List<BattleBroadcastEvent> events) {
    return events.stream().map(BattleBroadcastEventResponse::from).toList();
  }
}
