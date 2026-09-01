package com.somagochi.pochakfarm.badge.application;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.dto.BadgeResponse;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeQueryService {

  private final BadgeRepository badgeRepository;
  private final UserBadgeRepository userBadgeRepository;
  private final FileStorage fileStorage;

  @Transactional(readOnly = true)
  public Map<String, BadgeResponse> findAllByCodes(Collection<String> codes) {
    if (codes.isEmpty()) {
      return Map.of();
    }
    return badgeRepository.findByCodeIn(codes).stream()
        .collect(Collectors.toMap(Badge::getCode, this::toResponse));
  }

  @Transactional(readOnly = true)
  public Set<String> findOwnedBadgeCodes(Long userId) {
    return Set.copyOf(userBadgeRepository.findOwnedBadgeCodes(userId));
  }

  private BadgeResponse toResponse(Badge badge) {
    return new BadgeResponse(
        badge.getCode(),
        badge.getName(),
        badge.getDescription(),
        badge.getImageKey() == null ? null : fileStorage.buildUrl(badge.getImageKey()));
  }
}
