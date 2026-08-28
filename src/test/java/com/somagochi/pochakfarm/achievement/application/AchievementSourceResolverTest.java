package com.somagochi.pochakfarm.achievement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.entity.EntityChangeType;
import com.somagochi.pochakfarm.common.entity.EntityChangedEvent;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AchievementSourceResolverTest {

  @Test
  void animalCreationResolvesEveryCurrentlyRelatedMetric() {
    FarmSpaceRepository farmSpaceRepository = mock(FarmSpaceRepository.class);
    given(farmSpaceRepository.findById(2L))
        .willReturn(Optional.of(FarmSpace.create(1L, CardType.GROUND)));
    AnimalAchievementSourceResolver resolver =
        new AnimalAchievementSourceResolver(farmSpaceRepository);

    AchievementEvaluationRequest request =
        resolver
            .resolve(new EntityChangedEvent(Animal.create(3L, 2L, 1, 1), EntityChangeType.CREATED))
            .orElseThrow();

    assertEquals(1L, request.userId());
    assertEquals(Set.of(AchievementMetric.values()).size() - 1, request.metrics().size());
    assertTrue(request.metrics().contains(AchievementMetric.PLACED_ANIMAL_COUNT));
    assertTrue(request.metrics().contains(AchievementMetric.MAX_OWNED_COUNT_PER_TYPE));
    assertTrue(request.metrics().contains(AchievementMetric.OWNED_TYPE_COUNT));
    assertTrue(request.metrics().contains(AchievementMetric.ONLY_START_END_PLACED));
  }

  @Test
  void couponRecipientResolvesOnlyAfterConversionUpdate() {
    PreRegistrationAchievementSourceResolver resolver =
        new PreRegistrationAchievementSourceResolver();
    PreRegistrationCouponRecipient recipient = PreRegistrationCouponRecipient.issue(1L, 2L);
    recipient.assign(3L, 4L);

    assertTrue(
        resolver.resolve(new EntityChangedEvent(recipient, EntityChangeType.UPDATED)).isEmpty());

    recipient.convert(Instant.parse("2026-08-28T00:00:00Z"));
    AchievementEvaluationRequest request =
        resolver.resolve(new EntityChangedEvent(recipient, EntityChangeType.UPDATED)).orElseThrow();

    assertEquals(3L, request.userId());
    assertEquals(Set.of(AchievementMetric.PRE_REGISTRATION_CONVERTED), request.metrics());
  }
}
