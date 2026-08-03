package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementStats;
import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.dto.AnimalPlacement;
import com.somagochi.pochakfarm.coupon.application.CouponQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AchievementStatsLoader {

  private final CouponQueryService couponQueryService;
  private final AnimalQueryService animalQueryService;

  @Transactional(readOnly = true)
  public AchievementStats load(Long userId) {
    List<AnimalPlacement> placements = animalQueryService.getPlacements(userId);
    return new AchievementStats(
        couponQueryService.hasConvertedPreRegistrationCoupon(userId),
        placements.size(),
        animalQueryService.countOwnedByCardType(userId),
        hasSpaceWithOnlyStartAndEndPlaced(placements));
  }

  private boolean hasSpaceWithOnlyStartAndEndPlaced(List<AnimalPlacement> placements) {
    Map<Long, List<AnimalPlacement>> bySpace =
        placements.stream().collect(Collectors.groupingBy(AnimalPlacement::spaceId));
    return bySpace.values().stream().anyMatch(this::isOnlyStartAndEndPlaced);
  }

  private boolean isOnlyStartAndEndPlaced(Collection<AnimalPlacement> spacePlacements) {
    return spacePlacements.size() == 2
        && spacePlacements.stream()
            .anyMatch(placement -> placement.isAt(FarmSpace.FIRST_FLOOR, FarmSpace.FIRST_SLOT))
        && spacePlacements.stream()
            .anyMatch(
                placement ->
                    placement.isAt(FarmSpace.TOTAL_FLOOR_COUNT, FarmSpace.SLOT_COUNT_PER_FLOOR));
  }
}
