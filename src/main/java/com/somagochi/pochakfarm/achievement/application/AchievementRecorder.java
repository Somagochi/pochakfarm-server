package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AchievementRecorder {

  private final UserAchievementRepository userAchievementRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public List<UserAchievement> record(Long userId, Collection<Long> achievementIds) {
    if (achievementIds.isEmpty()) {
      return List.of();
    }
    List<UserAchievement> records =
        achievementIds.stream().map(id -> UserAchievement.achieve(userId, id)).toList();
    List<UserAchievement> saved = userAchievementRepository.saveAll(records);
    userAchievementRepository.flush();
    return saved;
  }
}
