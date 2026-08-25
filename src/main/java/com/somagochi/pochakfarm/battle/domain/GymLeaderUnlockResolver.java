package com.somagochi.pochakfarm.battle.domain;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GymLeaderUnlockResolver {

  private final BattlePolicy battlePolicy;

  public GymLeaderUnlock resolve(
      GymLeader gymLeader,
      GymLeader previousGymLeader,
      int userLevel,
      Set<String> ownedBadgeCodes) {
    int requiredLevel = battlePolicy.requiredLevel(gymLeader.getChallengeOrder());
    boolean levelSatisfied = userLevel >= requiredLevel;
    if (previousGymLeader == null) {
      return new GymLeaderUnlock(requiredLevel, levelSatisfied, null, true);
    }
    String previousBadgeCode = previousGymLeader.getBadgeCode();
    return new GymLeaderUnlock(
        requiredLevel,
        levelSatisfied,
        previousBadgeCode,
        ownedBadgeCodes.contains(previousBadgeCode));
  }
}
