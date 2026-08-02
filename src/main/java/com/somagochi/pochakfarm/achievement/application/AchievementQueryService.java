package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.AchievementStats;
import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import com.somagochi.pochakfarm.achievement.dto.AchievementResponse;
import com.somagochi.pochakfarm.achievement.dto.AchievementRewardResponse;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import com.somagochi.pochakfarm.common.response.CursorPage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementQueryService {

  private static final int PAGE_SIZE = 20;

  private final AchievementRepository achievementRepository;
  private final UserAchievementRepository userAchievementRepository;
  private final AchievementStatsLoader achievementStatsLoader;
  private final AchievementRecorder achievementRecorder;
  private final AchievementRewardCatalog achievementRewardCatalog;

  @Transactional(readOnly = true)
  public CursorPage<AchievementResponse> getAchievements(
      Long userId, AchievementCategory category, Long cursor) {
    AchievementStats stats = achievementStatsLoader.load(userId);
    List<Achievement> definitions = findValidDefinitions();
    Map<Long, UserAchievement> records = findRecords(userId);

    achievementRecorder
        .record(userId, findNewlyAchieved(definitions, records, stats))
        .forEach(record -> records.put(record.getAchievementId(), record));

    List<Achievement> fetched = fetchPage(definitions, records, category, cursor);
    boolean hasNext = fetched.size() > PAGE_SIZE;
    List<Achievement> page = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;
    return CursorPage.of(toResponses(page, stats, records), nextCursor(page, hasNext), hasNext);
  }

  private List<Achievement> fetchPage(
      List<Achievement> definitions,
      Map<Long, UserAchievement> records,
      AchievementCategory category,
      Long cursor) {
    long from = cursor == null ? Long.MIN_VALUE : cursor;
    return definitions.stream()
        .filter(definition -> category == null || definition.getCategory() == category)
        .filter(definition -> definition.isListedWhen(records.containsKey(definition.getId())))
        .filter(definition -> definition.getId() > from)
        .sorted(Comparator.comparing(Achievement::getCreatedAt).thenComparing(Achievement::getId))
        .limit(PAGE_SIZE + 1L)
        .toList();
  }

  private List<AchievementResponse> toResponses(
      List<Achievement> page, AchievementStats stats, Map<Long, UserAchievement> records) {
    Map<Long, List<AchievementRewardResponse>> rewards =
        achievementRewardCatalog.describeByAchievementIds(
            page.stream().map(Achievement::getId).toList());
    return page.stream()
        .map(
            definition ->
                AchievementResponse.of(
                    definition,
                    definition.progressOf(stats),
                    records.get(definition.getId()),
                    rewards.getOrDefault(definition.getId(), List.of())))
        .toList();
  }

  private Long nextCursor(List<Achievement> page, boolean hasNext) {
    return hasNext ? page.get(page.size() - 1).getId() : null;
  }

  private List<Achievement> findValidDefinitions() {
    List<Achievement> valid = new ArrayList<>();
    for (Achievement achievement : achievementRepository.findAll()) {
      if (achievement.isDefinitionValid()) {
        valid.add(achievement);
      } else {
        log.warn(
            "유효하지 않은 업적 정의 제외 code={} metric={} metricParam={} targetValue={}",
            achievement.getCode(),
            achievement.getMetric(),
            achievement.getMetricParam(),
            achievement.getTargetValue());
      }
    }
    return valid;
  }

  private Map<Long, UserAchievement> findRecords(Long userId) {
    Map<Long, UserAchievement> records = new HashMap<>();
    userAchievementRepository
        .findByUserId(userId)
        .forEach(record -> records.put(record.getAchievementId(), record));
    return records;
  }

  private List<Long> findNewlyAchieved(
      List<Achievement> definitions, Map<Long, UserAchievement> records, AchievementStats stats) {
    return definitions.stream()
        .filter(Achievement::isEnabled)
        .filter(definition -> !records.containsKey(definition.getId()))
        .filter(definition -> definition.isSatisfiedBy(stats))
        .map(Achievement::getId)
        .toList();
  }
}
