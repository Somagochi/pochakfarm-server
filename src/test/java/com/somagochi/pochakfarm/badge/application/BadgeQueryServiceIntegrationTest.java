package com.somagochi.pochakfarm.badge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.domain.BadgeCategory;
import com.somagochi.pochakfarm.badge.domain.UserBadge;
import com.somagochi.pochakfarm.badge.dto.OwnedBadgeResponse;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BadgeQueryServiceIntegrationTest {
  private static final Long USER_ID = 101L;

  @Autowired private BadgeQueryService service;
  @Autowired private BadgeRepository badges;
  @Autowired private UserBadgeRepository userBadges;
  @Autowired private FileStorage fileStorage;
  @Autowired private EntityManager entityManager;

  @Test
  void returnsOnlyOwnedBadgesWithDetailsAndAcquisitionTime() {
    Badge owned = badge("TEST_OWNED", BadgeCategory.GYM_LEADER, "badges/owned.png");
    UserBadge acquisition = userBadges.saveAndFlush(UserBadge.acquire(USER_ID, owned.getId()));
    Badge other = badge("TEST_OTHER", BadgeCategory.ACHIEVEMENT, null);
    userBadges.saveAndFlush(UserBadge.acquire(102L, other.getId()));
    badge("TEST_UNOWNED", BadgeCategory.ACHIEVEMENT, null);

    assertThat(service.getOwnedBadges(USER_ID, null, null))
        .isEqualTo(
            CursorPage.of(
                List.of(
                    new OwnedBadgeResponse(
                        owned.getCode(),
                        BadgeCategory.GYM_LEADER,
                        owned.getName(),
                        owned.getDescription(),
                        fileStorage.buildUrl(owned.getImageKey()),
                        acquisition.getAcquiredAt())),
                null,
                false));
  }

  @Test
  void returnsEmptyPageWhenNoBadgesAreOwned() {
    badge("TEST_UNOWNED", BadgeCategory.ACHIEVEMENT, null);
    assertThat(service.getOwnedBadges(USER_ID, null, null))
        .isEqualTo(CursorPage.of(List.<OwnedBadgeResponse>of(), null, false));
  }

  @Test
  void returnsNullImageUrlWhenImageKeyIsMissing() {
    acquire("TEST_NO_IMAGE", BadgeCategory.ACHIEVEMENT);
    assertThat(service.getOwnedBadges(USER_ID, null, null).content())
        .singleElement()
        .extracting(OwnedBadgeResponse::imageUrl)
        .isNull();
  }

  @Test
  void excludesDeletedBadgesAndDeletedAcquisitions() {
    Badge deleted = badge("TEST_DELETED", BadgeCategory.ACHIEVEMENT, null);
    userBadges.save(UserBadge.acquire(USER_ID, deleted.getId()));
    deleted.delete(Instant.now());
    Badge active = badge("TEST_ACTIVE", BadgeCategory.ACHIEVEMENT, null);
    UserBadge acquisition = userBadges.save(UserBadge.acquire(USER_ID, active.getId()));
    acquisition.delete(Instant.now());
    entityManager.flush();
    entityManager.clear();
    assertThat(service.getOwnedBadges(USER_ID, null, null).content()).isEmpty();
  }

  @Test
  void sortsByBadgeIdRegardlessOfAcquisitionOrder() {
    Badge first = badge("TEST_001", BadgeCategory.ACHIEVEMENT, null);
    Badge second = badge("TEST_002", BadgeCategory.ACHIEVEMENT, null);
    Badge third = badge("TEST_003", BadgeCategory.ACHIEVEMENT, null);
    for (Badge badge : List.of(third, first, second)) {
      userBadges.saveAndFlush(UserBadge.acquire(USER_ID, badge.getId()));
    }
    entityManager.clear();
    assertThat(service.getOwnedBadges(USER_ID, null, null).content())
        .extracting(OwnedBadgeResponse::code)
        .containsExactly("TEST_001", "TEST_002", "TEST_003");
  }

  @Test
  void filtersByCategoryWhenGiven() {
    acquire("TEST_ACH_1", BadgeCategory.ACHIEVEMENT);
    acquire("TEST_GYM_1", BadgeCategory.GYM_LEADER);
    acquire("TEST_ACH_2", BadgeCategory.ACHIEVEMENT);
    entityManager.clear();

    assertThat(service.getOwnedBadges(USER_ID, BadgeCategory.ACHIEVEMENT, null).content())
        .extracting(OwnedBadgeResponse::code)
        .containsExactly("TEST_ACH_1", "TEST_ACH_2");
    assertThat(service.getOwnedBadges(USER_ID, BadgeCategory.GYM_LEADER, null).content())
        .extracting(OwnedBadgeResponse::code, OwnedBadgeResponse::category)
        .containsExactly(tuple("TEST_GYM_1", BadgeCategory.GYM_LEADER));
    assertThat(service.getOwnedBadges(USER_ID, null, null).content()).hasSize(3);
  }

  @Test
  void pagesTwentyBadgesPerPageUsingLastBadgeIdAsCursor() {
    List<Badge> acquired =
        IntStream.rangeClosed(1, 25)
            .mapToObj(i -> acquire("TEST_%03d".formatted(i), BadgeCategory.ACHIEVEMENT))
            .toList();
    entityManager.clear();

    CursorPage<OwnedBadgeResponse> first = service.getOwnedBadges(USER_ID, null, null);
    assertThat(first.content()).hasSize(20);
    assertThat(first.content().getFirst().code()).isEqualTo("TEST_001");
    assertThat(first.hasNext()).isTrue();
    assertThat(first.nextCursor()).isEqualTo(acquired.get(19).getId());

    CursorPage<OwnedBadgeResponse> second =
        service.getOwnedBadges(USER_ID, null, first.nextCursor());
    assertThat(second.content())
        .extracting(OwnedBadgeResponse::code)
        .containsExactly("TEST_021", "TEST_022", "TEST_023", "TEST_024", "TEST_025");
    assertThat(second.hasNext()).isFalse();
    assertThat(second.nextCursor()).isNull();
  }

  @Test
  void returnsNoNextPageWhenExactlyOnePageIsOwned() {
    IntStream.rangeClosed(1, 20)
        .forEach(i -> acquire("TEST_%03d".formatted(i), BadgeCategory.GYM_LEADER));
    entityManager.clear();

    CursorPage<OwnedBadgeResponse> page =
        service.getOwnedBadges(USER_ID, BadgeCategory.GYM_LEADER, null);
    assertThat(page.content()).hasSize(20);
    assertThat(page.hasNext()).isFalse();
    assertThat(page.nextCursor()).isNull();
  }

  @Test
  void appliesCategoryFilterAcrossCursorPages() {
    List<Badge> acquired =
        IntStream.rangeClosed(1, 22)
            .mapToObj(
                i ->
                    acquire(
                        "TEST_%03d".formatted(i),
                        i % 2 == 0 ? BadgeCategory.GYM_LEADER : BadgeCategory.ACHIEVEMENT))
            .toList();
    entityManager.clear();

    CursorPage<OwnedBadgeResponse> page =
        service.getOwnedBadges(USER_ID, BadgeCategory.GYM_LEADER, acquired.get(9).getId());
    assertThat(page.content())
        .extracting(OwnedBadgeResponse::code)
        .containsExactly("TEST_012", "TEST_014", "TEST_016", "TEST_018", "TEST_020", "TEST_022");
    assertThat(page.hasNext()).isFalse();
  }

  private Badge acquire(String code, BadgeCategory category) {
    Badge badge = badge(code, category, null);
    userBadges.saveAndFlush(UserBadge.acquire(USER_ID, badge.getId()));
    return badge;
  }

  private Badge badge(String code, BadgeCategory category, String imageKey) {
    return badges.saveAndFlush(Badge.create(code, category, "뱃지 이름", "뱃지 설명", imageKey));
  }
}
