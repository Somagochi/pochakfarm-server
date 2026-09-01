package com.somagochi.pochakfarm.badge.infrastructure.persistence;

import com.somagochi.pochakfarm.badge.domain.UserBadge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

  boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

  List<UserBadge> findByUserId(Long userId);

  @Query(
      "select b.code from UserBadge ub, Badge b "
          + "where ub.badgeId = b.id and ub.userId = :userId")
  List<String> findOwnedBadgeCodes(@Param("userId") Long userId);
}
