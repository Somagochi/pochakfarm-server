package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
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
class CaptureRepositoryStaleProcessingTest {

  private static final Instant THRESHOLD = Instant.parse("2026-10-28T00:00:00Z");

  @Autowired private CaptureRepository captureRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void findsOnlyCapturesProcessingSinceBeforeThreshold() {
    User user = persistUser();
    Capture stale = persistCapture(user.getId());
    stale.markProcessing(THRESHOLD.minusSeconds(1));
    Capture recent = persistCapture(user.getId());
    recent.markProcessing(THRESHOLD);
    persistCapture(user.getId());
    Capture succeeded = persistCapture(user.getId());
    succeeded.markProcessing(THRESHOLD.minusSeconds(60));
    succeeded.succeed("public/a.png", "public/c.png", 100);
    Capture coupon =
        Capture.granted(
            user.getId(),
            CardType.GROUND,
            Tier.S,
            AnimalName.from("쿠폰냥"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "coupon-1",
            "public/card.png",
            "image/png",
            THRESHOLD.minusSeconds(3600));
    entityManager.persist(coupon);
    entityManager.flush();

    List<Long> ids =
        captureRepository.findStaleProcessingIds(GenerationStatus.PROCESSING, THRESHOLD);

    assertEquals(List.of(stale.getId()), ids);
  }

  private User persistUser() {
    User user =
        User.register(
            SocialProvider.KAKAO,
            "stale-" + UUID.randomUUID(),
            "stale@example.com",
            "u" + UUID.randomUUID().toString().substring(0, 5));
    entityManager.persist(user);
    return user;
  }

  private Capture persistCapture(Long userId) {
    Capture capture =
        Capture.create(
            userId,
            UUID.randomUUID().toString(),
            CardType.GROUND,
            Tier.C,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            UUID.randomUUID().toString(),
            "images/original.jpg",
            "image/jpeg",
            Instant.parse("2026-10-28T01:00:00Z"));
    entityManager.persist(capture);
    return capture;
  }
}
