package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.domain.TierProbability;
import com.somagochi.pochakfarm.capture.domain.TierSelectionPolicy;
import com.somagochi.pochakfarm.capture.dto.CaptureOverviewResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureTypeCount;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CaptureOverviewServiceTest {

  private static final Long USER_ID = 1L;

  @Mock private CaptureRepository captureRepository;
  @Mock private UserRepository userRepository;
  @Mock private TierSelectionPolicy tierSelectionPolicy;

  private CaptureOverviewService service;

  @BeforeEach
  void setUp() {
    service =
        new CaptureOverviewService(
            captureRepository, userRepository, new LevelRewardPolicy(), tierSelectionPolicy);
  }

  @Test
  void returnsLevelCountsInDisplayOrderAndTierProbabilities() {
    User user = userAt(12, 54);
    List<TierProbability> probabilities =
        List.of(
            probability(Tier.C, "44.9"),
            probability(Tier.B, "38"),
            probability(Tier.A, "14"),
            probability(Tier.S, "2.5"),
            probability(Tier.SS, "0.5"),
            probability(Tier.SSS, "0.1"));
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(captureRepository.countSucceededByCardType(USER_ID))
        .willReturn(
            List.of(
                new CaptureTypeCount(CardType.GROUND, 47),
                new CaptureTypeCount(CardType.SKY, 23),
                new CaptureTypeCount(CardType.SPACE, 1)));
    given(tierSelectionPolicy.probabilitiesFor(12)).willReturn(probabilities);

    CaptureOverviewResponse response = service.getOverview(USER_ID);

    assertEquals(new CaptureOverviewResponse.Level(12, 54, 150, 96), response.level());
    assertEquals(
        List.of(
            new CaptureOverviewResponse.CaptureCount(CardType.SKY, 23),
            new CaptureOverviewResponse.CaptureCount(CardType.GROUND, 47),
            new CaptureOverviewResponse.CaptureCount(CardType.SEA, 0),
            new CaptureOverviewResponse.CaptureCount(CardType.SPACE, 1)),
        response.captureCounts());
    assertEquals(probabilities, response.tierProbabilities());
  }

  @Test
  void returnsZeroRequiredAndRemainingExperienceAtMaximumLevel() {
    User user = userAt(50, 0);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(captureRepository.countSucceededByCardType(USER_ID)).willReturn(List.of());
    given(tierSelectionPolicy.probabilitiesFor(50)).willReturn(List.of());

    CaptureOverviewResponse response = service.getOverview(USER_ID);

    assertEquals(new CaptureOverviewResponse.Level(50, 0, 0, 0), response.level());
  }

  @Test
  void rejectsUnknownUser() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.getOverview(USER_ID));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }

  private User userAt(int level, long experience) {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com", "포착이");
    ReflectionTestUtils.setField(user, "level", level);
    ReflectionTestUtils.setField(user, "experience", experience);
    return user;
  }

  private TierProbability probability(Tier tier, String percent) {
    return new TierProbability(tier, new BigDecimal(percent));
  }
}
