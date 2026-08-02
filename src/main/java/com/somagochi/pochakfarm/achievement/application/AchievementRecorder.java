package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementRecorder {

  private final UserAchievementRepository userAchievementRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<UserAchievement> record(Long userId, Collection<Long> achievementIds) {
    if (achievementIds.isEmpty()) {
      return List.of();
    }
    try {
      List<UserAchievement> records =
          achievementIds.stream().map(id -> UserAchievement.achieve(userId, id)).toList();
      List<UserAchievement> saved = userAchievementRepository.saveAll(records);
      userAchievementRepository.flush();
      return saved;
    } catch (DataIntegrityViolationException e) {
      log.warn("업적 달성 기록 저장 실패 userId={} achievementIds={}", userId, achievementIds, e);
      return List.of();
    }
  }
}
