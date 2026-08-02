package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementStats;
import com.somagochi.pochakfarm.capture.application.CaptureQueryService;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureCount;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AchievementStatsLoader {

  private final UserQueryService userQueryService;
  private final CaptureQueryService captureQueryService;

  @Transactional(readOnly = true)
  public AchievementStats load(Long userId) {
    int userLevel = userQueryService.getLevel(userId);
    List<CaptureCount> counts = captureQueryService.countCapturedByCardTypeAndTier(userId);
    Map<CardType, Long> byCardType = new EnumMap<>(CardType.class);
    Map<Tier, Long> byTier = new EnumMap<>(Tier.class);
    for (CaptureCount count : counts) {
      byCardType.merge(count.cardType(), count.count(), Long::sum);
      byTier.merge(count.tier(), count.count(), Long::sum);
    }
    return new AchievementStats(userLevel, byCardType, byTier);
  }
}
