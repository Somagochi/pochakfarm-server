package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CapturePersistenceTest {

  @Autowired private EntityManager entityManager;

  @Test
  void persistsRequiredUserAndStringStatusesWithNullableAsyncResults() {
    User user = persistUser();
    Capture capture = Capture.start(user.getId(), CardType.GROUND, Tier.C);

    entityManager.persist(capture);
    entityManager.flush();

    Object[] row =
        (Object[])
            entityManager
                .createNativeQuery(
                    """
                    SELECT generation_status, game_status, animal_name, skill_1, skill_2,
                           card_no, card_image, animal_image, elapsed_ms, failure_reason
                    FROM captures
                    WHERE id = :id
                    """)
                .setParameter("id", capture.getId())
                .getSingleResult();

    assertEquals(GenerationStatus.WAITING_UPLOAD.name(), row[0].toString());
    assertEquals(GameStatus.PENDING.name(), row[1].toString());
    for (int index = 2; index < row.length; index++) {
      assertNull(row[index]);
    }
  }

  @Test
  void requiresUser() {
    assertThrows(NullPointerException.class, () -> Capture.start(null, CardType.GROUND, Tier.C));
  }

  @Test
  void withdrawingUserDoesNotDeleteCapture() {
    User user = persistUser();
    Capture capture = Capture.start(user.getId(), CardType.SKY, Tier.B);
    entityManager.persist(capture);
    entityManager.flush();

    user.withdraw();
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

  private User persistUser() {
    User user =
        User.register(
            SocialProvider.KAKAO, "capture-test-" + UUID.randomUUID(), "capture@test.com");
    entityManager.persist(user);
    return user;
  }
}
