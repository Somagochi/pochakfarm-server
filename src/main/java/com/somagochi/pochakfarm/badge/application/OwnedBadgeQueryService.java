package com.somagochi.pochakfarm.badge.application;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.domain.BadgeCategory;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository.OwnedBadgeView;
import com.somagochi.pochakfarm.battle.application.GymLeaderQueryService;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnedBadgeQueryService {

  private static final int PAGE_SIZE = 20;

  private final UserBadgeRepository userBadgeRepository;
  private final GymLeaderQueryService gymLeaderQueryService;
  private final FileStorage fileStorage;

  @Transactional(readOnly = true)
  public CursorPage<OwnedBadgeResponse> getOwnedBadges(
      Long userId, BadgeCategory category, Long cursor) {
    List<OwnedBadgeView> fetched =
        userBadgeRepository.findOwnedBadges(
            userId,
            category == null ? EnumSet.allOf(BadgeCategory.class) : EnumSet.of(category),
            cursor == null ? Long.MIN_VALUE : cursor,
            Limit.of(PAGE_SIZE + 1));
    boolean hasNext = fetched.size() > PAGE_SIZE;
    List<OwnedBadgeView> page = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;
    Long nextCursor = hasNext ? page.get(page.size() - 1).getBadge().getId() : null;
    Map<String, String> thumbnailUrls = findGymLeaderThumbnailUrls(page);
    return CursorPage.of(
        page.stream().map(owned -> toResponse(owned, thumbnailUrls)).toList(), nextCursor, hasNext);
  }

  private Map<String, String> findGymLeaderThumbnailUrls(List<OwnedBadgeView> page) {
    List<String> gymLeaderBadgeCodes =
        page.stream()
            .map(OwnedBadgeView::getBadge)
            .filter(badge -> badge.getCategory() == BadgeCategory.GYM_LEADER)
            .map(Badge::getCode)
            .toList();
    return gymLeaderQueryService.findThumbnailUrlsByBadgeCodes(gymLeaderBadgeCodes);
  }

  private OwnedBadgeResponse toResponse(OwnedBadgeView owned, Map<String, String> thumbnailUrls) {
    Badge badge = owned.getBadge();
    return new OwnedBadgeResponse(
        badge.getCode(),
        badge.getCategory(),
        badge.getName(),
        badge.getDescription(),
        badge.getImageKey() == null ? null : fileStorage.buildUrl(badge.getImageKey()),
        thumbnailUrls.get(badge.getCode()),
        owned.getAcquiredAt());
  }
}
