package com.somagochi.pochakfarm.badge.infrastructure.persistence;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.domain.BadgeCategory;
import com.somagochi.pochakfarm.badge.domain.UserBadge;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

  @Query(
      "select b as badge, ub.createdAt as acquiredAt from UserBadge ub, Badge b "
          + "where ub.badgeId = b.id and ub.userId = :userId "
          + "and b.category in :categories and b.code > :cursor "
          + "order by b.code asc")
  List<OwnedBadgeView> findOwnedBadges(
      @Param("userId") Long userId,
      @Param("categories") Collection<BadgeCategory> categories,
      @Param("cursor") String cursor,
      Limit limit);

  interface OwnedBadgeView {
    Badge getBadge();

    Instant getAcquiredAt();
  }

  boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

  List<UserBadge> findByUserId(Long userId);

  @Query(
      "select b.code from UserBadge ub, Badge b "
          + "where ub.badgeId = b.id and ub.userId = :userId")
  List<String> findOwnedBadgeCodes(@Param("userId") Long userId);
}
