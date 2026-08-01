package com.somagochi.pochakfarm.achievement.infrastructure.persistence;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRewardRepository extends JpaRepository<AchievementReward, Long> {

  List<AchievementReward> findByAchievementIdIn(Collection<Long> achievementIds);

  List<AchievementReward> findByAchievementId(Long achievementId);
}
