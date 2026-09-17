package com.somagochi.pochakfarm.badge.application;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.domain.BadgeCategory;
import com.somagochi.pochakfarm.badge.dto.BadgeResponse;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgePage;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository.OwnedBadgeView;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeQueryService {

  private static final int PAGE_SIZE = 20;

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
  public BadgeResponse getByCode(String code) {
    return badgeRepository
        .findByCode(code)
        .map(this::toResponse)
        .orElseThrow(() -> new BusinessException(ErrorCode.BADGE_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public Set<String> findOwnedBadgeCodes(Long userId) {
    return Set.copyOf(userBadgeRepository.findOwnedBadgeCodes(userId));
  }

  @Transactional(readOnly = true)
  public OwnedBadgePage getOwnedBadges(Long userId, BadgeCategory category, String cursor) {
    List<OwnedBadgeView> fetched =
        userBadgeRepository.findOwnedBadges(
            userId,
            category == null ? EnumSet.allOf(BadgeCategory.class) : EnumSet.of(category),
            cursor == null ? "" : cursor,
            Limit.of(PAGE_SIZE + 1));
    boolean hasNext = fetched.size() > PAGE_SIZE;
    List<OwnedBadgeView> page = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;
    String nextCursor = hasNext ? page.get(page.size() - 1).getBadge().getCode() : null;
    return new OwnedBadgePage(
        page.stream().map(this::toOwnedResponse).toList(), nextCursor, hasNext);
  }

  private OwnedBadgeResponse toOwnedResponse(OwnedBadgeView owned) {
    Badge badge = owned.getBadge();
    BadgeResponse response = toResponse(badge);
    return new OwnedBadgeResponse(
        response.code(),
        badge.getCategory(),
        response.name(),
        response.description(),
        response.imageUrl(),
        owned.getAcquiredAt());
  }

  private BadgeResponse toResponse(Badge badge) {
    return new BadgeResponse(
        badge.getCode(),
        badge.getName(),
        badge.getDescription(),
        badge.getImageKey() == null ? null : fileStorage.buildUrl(badge.getImageKey()));
  }
}
