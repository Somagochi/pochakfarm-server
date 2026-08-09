package com.somagochi.pochakfarm.badge.infrastructure.persistence;

import com.somagochi.pochakfarm.badge.domain.UserBadge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

  boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

  List<UserBadge> findByUserId(Long userId);
}
