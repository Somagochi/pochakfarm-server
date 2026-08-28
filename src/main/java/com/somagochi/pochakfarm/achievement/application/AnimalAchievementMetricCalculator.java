package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.dto.AnimalPlacement;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnimalAchievementMetricCalculator implements AchievementMetricCalculator {

  private static final Set<AchievementMetric> SUPPORTED_METRICS =
      Set.of(
          AchievementMetric.PLACED_ANIMAL_COUNT,
          AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
          AchievementMetric.OWNED_TYPE_COUNT,
          AchievementMetric.ONLY_START_END_PLACED);

  private final AnimalQueryService animalQueryService;

  @Override
  public Set<AchievementMetric> supportedMetrics() {
    return SUPPORTED_METRICS;
  }

  @Override
  public Map<AchievementMetric, Long> calculate(
      Long userId, Set<AchievementMetric> requestedMetrics) {
    Map<AchievementMetric, Long> values = new EnumMap<>(AchievementMetric.class);
    boolean needsPlacements =
        requestedMetrics.contains(AchievementMetric.PLACED_ANIMAL_COUNT)
            || requestedMetrics.contains(AchievementMetric.ONLY_START_END_PLACED);
    boolean needsOwnedCounts =
        requestedMetrics.contains(AchievementMetric.MAX_OWNED_COUNT_PER_TYPE)
            || requestedMetrics.contains(AchievementMetric.OWNED_TYPE_COUNT);

    if (needsPlacements) {
      List<AnimalPlacement> placements = animalQueryService.getPlacements(userId);
      if (requestedMetrics.contains(AchievementMetric.PLACED_ANIMAL_COUNT)) {
        values.put(AchievementMetric.PLACED_ANIMAL_COUNT, (long) placements.size());
      }
      if (requestedMetrics.contains(AchievementMetric.ONLY_START_END_PLACED)) {
        values.put(
            AchievementMetric.ONLY_START_END_PLACED,
            hasSpaceWithOnlyStartAndEndPlaced(placements) ? 1L : 0L);
      }
    }

    if (needsOwnedCounts) {
      Map<?, Long> ownedCounts = animalQueryService.countOwnedByCardType(userId);
      if (requestedMetrics.contains(AchievementMetric.MAX_OWNED_COUNT_PER_TYPE)) {
        values.put(
            AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
            ownedCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L));
      }
      if (requestedMetrics.contains(AchievementMetric.OWNED_TYPE_COUNT)) {
        values.put(AchievementMetric.OWNED_TYPE_COUNT, (long) ownedCounts.size());
      }
    }
    return values;
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
