package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureCount;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CaptureQueryServiceCountTest {

  private static final Instant EXPIRES_AT = Instant.parse("2026-07-29T00:00:00Z");

  @Autowired private EntityManager entityManager;
  @Autowired private CaptureQueryService captureQueryService;

  @Test
  void aggregatesCardTypeAndTierFromSingleGroupByQuery() {
    Long userId = persistUser();
    captured(userId, CardType.SEA, Tier.S);
    captured(userId, CardType.SEA, Tier.S);
    captured(userId, CardType.SEA, Tier.SSS);
    captured(userId, CardType.SKY, Tier.S);
    entityManager.flush();
    entityManager.clear();

    List<CaptureCount> counts = captureQueryService.countCapturedByCardTypeAndTier(userId);

    assertEquals(4L, counts.stream().mapToLong(CaptureCount::count).sum());
    assertEquals(3L, countOf(counts, CardType.SEA));
    assertEquals(1L, countOf(counts, CardType.SKY));
    assertEquals(3L, countOf(counts, Tier.S));
    assertEquals(1L, countOf(counts, Tier.SSS));
  }

  @Test
  void excludesPendingCapturesAndOtherUsers() {
    Long userId = persistUser();
    Long otherUserId = persistUser();
    captured(userId, CardType.GROUND, Tier.A);
    pending(userId, CardType.GROUND, Tier.A);
    captured(otherUserId, CardType.GROUND, Tier.A);
    entityManager.flush();
    entityManager.clear();

    List<CaptureCount> counts = captureQueryService.countCapturedByCardTypeAndTier(userId);

    assertEquals(1L, counts.stream().mapToLong(CaptureCount::count).sum());
  }

  @Test
  void excludesSoftDeletedCaptures() {
    Long userId = persistUser();
    Capture deleted = captured(userId, CardType.SPACE, Tier.SS);
    entityManager
        .createNativeQuery("update captures set deleted_at = current_timestamp where id = :id")
        .setParameter("id", deleted.getId())
        .executeUpdate();
    entityManager.clear();

    List<CaptureCount> counts = captureQueryService.countCapturedByCardTypeAndTier(userId);

    assertTrue(counts.isEmpty());
  }

  private long countOf(List<CaptureCount> counts, CardType cardType) {
    return counts.stream()
        .filter(count -> count.cardType() == cardType)
        .mapToLong(CaptureCount::count)
        .sum();
  }

  private long countOf(List<CaptureCount> counts, Tier tier) {
    return counts.stream()
        .filter(count -> count.tier() == tier)
        .mapToLong(CaptureCount::count)
        .sum();
  }

  private Capture captured(Long userId, CardType cardType, Tier tier) {
    Capture capture = pending(userId, cardType, tier);
    entityManager.flush();
    entityManager
        .createNativeQuery("update captures set game_status = 'SUCCEEDED' where id = :id")
        .setParameter("id", capture.getId())
        .executeUpdate();
    return capture;
  }

  private Capture pending(Long userId, CardType cardType, Tier tier) {
    Capture capture =
        Capture.create(
            userId,
            UUID.randomUUID().toString(),
            cardType,
            tier,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "123",
            "origin/" + UUID.randomUUID(),
            "image/png",
            EXPIRES_AT);
    entityManager.persist(capture);
    return capture;
  }

  private Long persistUser() {
    User user =
        User.register(SocialProvider.KAKAO, UUID.randomUUID().toString(), UUID.randomUUID() + "@t");
    entityManager.persist(user);
    return user.getId();
  }
}
