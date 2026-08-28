package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.coupon.application.CouponQueryService;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PreRegistrationAchievementMetricCalculator implements AchievementMetricCalculator {

  private static final Set<AchievementMetric> SUPPORTED_METRICS =
      Set.of(AchievementMetric.PRE_REGISTRATION_CONVERTED);

  private final CouponQueryService couponQueryService;

  @Override
  public Set<AchievementMetric> supportedMetrics() {
    return SUPPORTED_METRICS;
  }

  @Override
  public Map<AchievementMetric, Long> calculate(
      Long userId, Set<AchievementMetric> requestedMetrics) {
    if (!requestedMetrics.contains(AchievementMetric.PRE_REGISTRATION_CONVERTED)) {
      return Map.of();
    }
    return Map.of(
        AchievementMetric.PRE_REGISTRATION_CONVERTED,
        couponQueryService.hasConvertedPreRegistrationCoupon(userId) ? 1L : 0L);
  }
}
