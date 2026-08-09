package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureTypeCount;
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
class CaptureRepositoryOverviewTest {

  @Autowired private CaptureRepository captureRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void countsOnlySucceededCapturesForRequestedUserIncludingCouponGrants() {
    User user = persistUser("owner");
    User anotherUser = persistUser("another");
    persistCompletedCapture(user.getId(), CardType.SKY, GameStatus.SUCCEEDED);
    persistCompletedCapture(user.getId(), CardType.SKY, GameStatus.SUCCEEDED);
    persistCompletedCapture(user.getId(), CardType.GROUND, GameStatus.FAILED);
    persistPendingCapture(user.getId(), CardType.SEA);
    persistCompletedCapture(anotherUser.getId(), CardType.SPACE, GameStatus.SUCCEEDED);
    Capture coupon =
        Capture.granted(
            user.getId(),
            CardType.GROUND,
            Tier.A,
            AnimalName.from("쿠폰냥"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "coupon-1",
            "public/card.png",
            "image/png",
            Instant.parse("2026-08-04T00:00:00Z"));
    coupon.completeGrant("public/animal.png");
    entityManager.persist(coupon);
    entityManager.flush();

    List<CaptureTypeCount> counts = captureRepository.countSucceededByCardType(user.getId());

    assertEquals(2, countOf(counts, CardType.SKY));
    assertEquals(1, countOf(counts, CardType.GROUND));
    assertEquals(0, countOf(counts, CardType.SEA));
    assertEquals(0, countOf(counts, CardType.SPACE));
  }

  private long countOf(List<CaptureTypeCount> counts, CardType cardType) {
    return counts.stream()
        .filter(count -> count.cardType() == cardType)
        .mapToLong(CaptureTypeCount::count)
        .findFirst()
        .orElse(0);
  }

  private User persistUser(String suffix) {
    User user =
        User.register(
            SocialProvider.KAKAO,
            "overview-" + suffix + "-" + UUID.randomUUID(),
            suffix + "@example.com");
    entityManager.persist(user);
    return user;
  }

  private void persistCompletedCapture(Long userId, CardType cardType, GameStatus status) {
    Capture capture = persistPendingCapture(userId, cardType);
    capture.completeGame(status, status == GameStatus.SUCCEEDED ? 10 : 0);
  }

  private Capture persistPendingCapture(Long userId, CardType cardType) {
    Capture capture =
        Capture.create(
            userId,
            UUID.randomUUID().toString(),
            cardType,
            Tier.C,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            UUID.randomUUID().toString(),
            "images/original.jpg",
            "image/jpeg",
            Instant.parse("2026-08-04T01:00:00Z"));
    entityManager.persist(capture);
    return capture;
  }
}
