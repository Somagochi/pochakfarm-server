package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "최종 승부 및 대전 결과")
public record BattleFinalRoundResultResponse(
    Long battleId,
    BattleStatus battleStatus,
    BattleResult battleResult,
    int barPosition,
    BattleFinalRoundStateResponse finalRound,
    BattleRewardResponse reward) {

  public static BattleFinalRoundResultResponse from(
      Battle battle, BattlePolicy policy, BattleRewardResponse reward) {
    return new BattleFinalRoundResultResponse(
        battle.getId(),
        battle.getStatus(),
        battle.getResult(),
        battle.getBarPosition(),
        BattleFinalRoundStateResponse.from(battle, policy),
        reward);
  }
}
