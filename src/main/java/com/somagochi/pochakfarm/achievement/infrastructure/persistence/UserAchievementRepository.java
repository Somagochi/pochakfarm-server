package com.somagochi.pochakfarm.achievement.infrastructure.persistence;

import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

  List<UserAchievement> findByUserId(Long userId);

  Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);

  @Modifying
  @Query(
      "update UserAchievement ua set ua.rewardClaimedAt = :claimedAt "
          + "where ua.id = :id and ua.rewardClaimedAt is null")
  int markClaimed(@Param("id") Long id, @Param("claimedAt") Instant claimedAt);
}
