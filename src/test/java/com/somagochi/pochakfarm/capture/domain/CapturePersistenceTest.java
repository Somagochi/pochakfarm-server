package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CapturePersistenceTest {

  private static final Instant GAME_RESULT_EXPIRES_AT = Instant.parse("2026-07-24T01:05:00Z");

  @Autowired private EntityManager entityManager;

  @Test
  void persistsRequiredUserAndStringStatusesWithNullableAsyncResults() {
    User user = persistUser();
    Capture capture = capture(user.getId(), CardType.GROUND, Tier.C);

    entityManager.persist(capture);
    entityManager.flush();

    Object[] row =
        (Object[])
            entityManager
                .createNativeQuery(
                    """
                    SELECT generation_status, game_status, client_request_id, original_image_key,
                           original_image_content_type, animal_name, skill_1, skill_2,
                           card_no, card_image, animal_image, elapsed_ms, failure_reason
                    FROM captures
                    WHERE id = :id
                    """)
                .setParameter("id", capture.getId())
                .getSingleResult();

    assertEquals(GenerationStatus.WAITING_UPLOAD.name(), row[0].toString());
    assertEquals(GameStatus.PENDING.name(), row[1].toString());
    assertEquals("550e8400-e29b-41d4-a716-446655440000", row[2]);
    assertEquals("images/capture-original/1/original.jpg", row[3]);
    assertEquals("image/jpeg", row[4]);
    assertEquals("두부", row[5]);
    assertEquals(CardSkill.GROUND_PAW_STRIKE.name(), row[6].toString());
    assertEquals(CardSkill.GROUND_LEAF_GUARD.name(), row[7].toString());
    assertEquals("001", row[8]);
    for (int index = 9; index < row.length; index++) {
      assertNull(row[index]);
    }

    entityManager.clear();
    assertEquals(
        GAME_RESULT_EXPIRES_AT,
        entityManager.find(Capture.class, capture.getId()).getGameResultExpiresAt());
  }

  @Test
  void requiresUser() {
    assertThrows(
        NullPointerException.class,
        () ->
            Capture.create(
                null,
                "550e8400-e29b-41d4-a716-446655440000",
                CardType.GROUND,
                Tier.C,
                AnimalName.from("두부"),
                CardSkill.GROUND_PAW_STRIKE,
                CardSkill.GROUND_LEAF_GUARD,
                "001",
                "images/capture-original/1/original.jpg",
                "image/jpeg",
                GAME_RESULT_EXPIRES_AT));
  }

  @Test
  void withdrawingUserDoesNotDeleteCapture() {
    User user = persistUser();
    Capture capture = capture(user.getId(), CardType.SKY, Tier.B);
    entityManager.persist(capture);
    entityManager.flush();

    user.withdraw(null);
    entityManager.flush();
    entityManager.clear();

    Number count =
        (Number)
            entityManager
                .createNativeQuery("SELECT COUNT(*) FROM captures WHERE id = :id")
                .setParameter("id", capture.getId())
                .getSingleResult();

    assertEquals(1L, count.longValue());
  }

  @Test
  void requiresUniqueClientRequestIdPerUser() {
    User user = persistUser();
    entityManager.persist(capture(user.getId(), CardType.GROUND, Tier.C));
    entityManager.flush();

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(capture(user.getId(), CardType.SKY, Tier.B));
          entityManager.flush();
        });
  }

  @Test
  void persistsFinalGameResultAndGrantedExperience() {
    User user = persistUser();
    Capture capture = capture(user.getId(), CardType.GROUND, Tier.C);
    capture.completeGame(GameStatus.SUCCEEDED, 10);

    entityManager.persist(capture);
    entityManager.flush();
    entityManager.clear();

    Capture saved = entityManager.find(Capture.class, capture.getId());
    assertEquals(GameStatus.SUCCEEDED, saved.getGameStatus());
    assertEquals(10, saved.getGrantedExperience());
  }

  @Test
  void persistsCardAndAnimalImagesInSeparateColumns() {
    User user = persistUser();
    Capture capture = capture(user.getId(), CardType.GROUND, Tier.C);
    capture.succeed("public/capture-animal/1/123.png", "public/card.png", 100);

    entityManager.persist(capture);
    entityManager.flush();

    Object[] row =
        (Object[])
            entityManager
                .createNativeQuery("SELECT card_image, animal_image FROM captures WHERE id = :id")
                .setParameter("id", capture.getId())
                .getSingleResult();
    assertEquals("public/card.png", row[0]);
    assertEquals("public/capture-animal/1/123.png", row[1]);
  }

  private User persistUser() {
    User user =
        User.register(
            SocialProvider.KAKAO, "capture-test-" + UUID.randomUUID(), "capture@test.com");
    assertEquals(1, user.getLevel());
    entityManager.persist(user);
    return user;
  }

  private Capture capture(Long userId, CardType cardType, Tier tier) {
    return Capture.create(
        userId,
        "550e8400-e29b-41d4-a716-446655440000",
        cardType,
        tier,
        AnimalName.from("두부"),
        CardSkill.GROUND_PAW_STRIKE,
        CardSkill.GROUND_LEAF_GUARD,
        "001",
        "images/capture-original/1/original.jpg",
        "image/jpeg",
        GAME_RESULT_EXPIRES_AT);
  }
}
