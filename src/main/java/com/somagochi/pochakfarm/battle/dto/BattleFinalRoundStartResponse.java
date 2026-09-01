package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "최종 승부 시작 결과")
public record BattleFinalRoundStartResponse(
    Long battleId,
    BattleStatus battleStatus,
    BattleResult battleResult,
    BattleFinalRoundStateResponse finalRound,
    BattleRewardResponse reward) {

  public static BattleFinalRoundStartResponse from(
      Battle battle, BattlePolicy policy, BattleRewardResponse reward) {
    return new BattleFinalRoundStartResponse(
        battle.getId(),
        battle.getStatus(),
        battle.getResult(),
        BattleFinalRoundStateResponse.from(battle, policy),
        reward);
  }
}
