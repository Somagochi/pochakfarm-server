package com.somagochi.pochakfarm.badge.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.domain.UserBadge;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BadgeQueryServiceIntegrationTest {
  @Autowired private BadgeQueryService service;
  @Autowired private BadgeRepository badges;
  @Autowired private UserBadgeRepository userBadges;
  @Autowired private FileStorage fileStorage;
  @Autowired private EntityManager entityManager;

  @Test
  void returnsOnlyOwnedBadgesWithDetailsAndAcquisitionTime() {
    Badge owned = badge("TEST_OWNED", "badges/owned.png");
    UserBadge acquisition = userBadges.saveAndFlush(UserBadge.acquire(101L, owned.getId()));
    Badge other = badge("TEST_OTHER", null);
    userBadges.saveAndFlush(UserBadge.acquire(102L, other.getId()));
    badge("TEST_UNOWNED", null);

    assertThat(service.getOwnedBadges(101L))
        .containsExactly(
            new OwnedBadgeResponse(
                owned.getCode(),
                owned.getName(),
                owned.getDescription(),
                fileStorage.buildUrl(owned.getImageKey()),
                acquisition.getAcquiredAt()));
  }

  @Test
  void returnsEmptyListWhenNoBadgesAreOwned() {
    badge("TEST_UNOWNED", null);
    assertThat(service.getOwnedBadges(101L)).isEmpty();
  }

  @Test
  void returnsNullImageUrlWhenImageKeyIsMissing() {
    Badge badge = badge("TEST_NO_IMAGE", null);
    userBadges.saveAndFlush(UserBadge.acquire(101L, badge.getId()));
    assertThat(service.getOwnedBadges(101L))
        .singleElement()
        .extracting(OwnedBadgeResponse::imageUrl)
        .isNull();
  }

  @Test
  void excludesDeletedBadgesAndDeletedAcquisitions() {
    Badge deleted = badge("TEST_DELETED", null);
    userBadges.save(UserBadge.acquire(101L, deleted.getId()));
    deleted.delete(Instant.now());
    Badge active = badge("TEST_ACTIVE", null);
    UserBadge acquisition = userBadges.save(UserBadge.acquire(101L, active.getId()));
    acquisition.delete(Instant.now());
    entityManager.flush();
    entityManager.clear();
    assertThat(service.getOwnedBadges(101L)).isEmpty();
  }

  @Test
  void sortsByBadgeCodeRegardlessOfAcquisitionOrder() {
    for (String code : java.util.List.of("TEST_003", "TEST_001", "TEST_002")) {
      Badge badge = badge(code, null);
      userBadges.saveAndFlush(UserBadge.acquire(101L, badge.getId()));
    }
    entityManager.clear();
    assertThat(service.getOwnedBadges(101L))
        .extracting(OwnedBadgeResponse::code)
        .containsExactly("TEST_001", "TEST_002", "TEST_003");
  }

  private Badge badge(String code, String imageKey) {
    return badges.saveAndFlush(Badge.create(code, "뱃지 이름", "뱃지 설명", imageKey));
  }
}
