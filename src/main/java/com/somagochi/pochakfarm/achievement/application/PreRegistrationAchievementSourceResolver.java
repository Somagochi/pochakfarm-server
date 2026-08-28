package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementSource;
import com.somagochi.pochakfarm.common.entity.EntityChangeType;
import com.somagochi.pochakfarm.common.entity.EntityChangedEvent;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationAchievementSourceResolver implements AchievementSourceResolver {

  private static final Set<Class<? extends AchievementSource>> SOURCE_TYPES =
      Set.of(PreRegistrationCouponRecipient.class);

  @Override
  public Set<Class<? extends AchievementSource>> sourceTypes() {
    return SOURCE_TYPES;
  }

  @Override
  public Optional<AchievementEvaluationRequest> resolve(EntityChangedEvent event) {
    if (event.changeType() != EntityChangeType.UPDATED
        || !(event.entity() instanceof PreRegistrationCouponRecipient recipient)
        || !recipient.isConverted()
        || recipient.getUserId() == null) {
      return Optional.empty();
    }
    return Optional.of(
        new AchievementEvaluationRequest(
            recipient.getUserId(), Set.of(AchievementMetric.PRE_REGISTRATION_CONVERTED)));
  }
}
