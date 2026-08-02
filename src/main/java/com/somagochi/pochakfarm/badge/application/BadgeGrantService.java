package com.somagochi.pochakfarm.badge.application;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.domain.UserBadge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeGrantService {

  private final BadgeRepository badgeRepository;
  private final UserBadgeRepository userBadgeRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public void grant(Long userId, String badgeCode) {
    Badge badge =
        badgeRepository
            .findByCode(badgeCode)
            .orElseThrow(() -> new BusinessException(ErrorCode.BADGE_NOT_FOUND));
    if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
      return;
    }
    userBadgeRepository.save(UserBadge.acquire(userId, badge.getId()));
  }
}
