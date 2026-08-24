package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BattleAdvantageResolver {

  private final BattlePolicy battlePolicy;

  public BattleAdvantageResolver(BattlePolicy battlePolicy) {
    this.battlePolicy = battlePolicy;
  }

  public BattlePositionChange resolveTier(BattlePosition position, Tier userTier, Tier npcTier) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(userTier);
    Objects.requireNonNull(npcTier);
    return position.move(battlePolicy.tierPointDifference(userTier, npcTier));
  }

  public BattlePositionChange resolveType(
      BattlePosition position, CardType userType, CardType npcType) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(userType);
    Objects.requireNonNull(npcType);
    return position.move(battlePolicy.typePointDifference(userType, npcType));
  }
}
