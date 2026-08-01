package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.CaptureExperiencePolicy;
import com.somagochi.pochakfarm.capture.domain.CaptureGameResultPolicy;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest.ThrowResult;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CaptureGameResultServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long CAPTURE_ID = 123L;
  private static final Instant NOW = Instant.parse("2026-08-01T05:00:00Z");

  @Mock private CaptureRepository captureRepository;
  @Mock private UserRepository userRepository;

  private CaptureGameResultService service;

  @BeforeEach
  void setUp() {
    service =
        new CaptureGameResultService(
            captureRepository,
            userRepository,
            new CaptureGameResultPolicy(),
            new CaptureExperiencePolicy(),
            new LevelRewardPolicy(),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void finalizesSuccessAndAppliesRewardAtAnyGenerationStatus() {
    Capture capture = capture(NOW.plusSeconds(1));
    capture.fail("AI_ERROR");
    User user = user();
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

    CaptureGameResultResponse response =
        service.submit(USER_ID, CAPTURE_ID, request(new ThrowResult(1, true)));

    assertEquals(GameStatus.SUCCEEDED, response.gameStatus());
    assertEquals(10, response.reward().experienceReward());
    assertEquals(1, response.progression().before().level());
    assertEquals(0, response.progression().before().experience());
    assertEquals(1, response.progression().after().level());
    assertEquals(10, response.progression().after().experience());
    assertEquals(40, response.progression().after().requiredExperienceForNextLevel());
    assertEquals(10, user.getExperience());
    assertEquals(GenerationStatus.FAILED, capture.getGenerationStatus());
    verify(userRepository).findByIdForUpdate(USER_ID);
  }

  @Test
  void rewardsFailureOnlyAfterThreeThrows() {
    Capture capture = capture(NOW.plusSeconds(1));
    User user = user();
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

    CaptureGameResultResponse response =
        service.submit(
            USER_ID,
            CAPTURE_ID,
            request(
                new ThrowResult(1, false), new ThrowResult(2, false), new ThrowResult(3, false)));

    assertEquals(GameStatus.FAILED, response.gameStatus());
    assertEquals(2, response.reward().experienceReward());
    assertEquals(2, user.getExperience());
  }

  @Test
  void appliesLevelUpCoinAndReturnsProgression() {
    Capture capture = capture(NOW.plusSeconds(1));
    User user = user();
    ReflectionTestUtils.setField(user, "experience", 39L);
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

    CaptureGameResultResponse response =
        service.submit(USER_ID, CAPTURE_ID, request(new ThrowResult(1, true)));

    assertEquals(2, user.getLevel());
    assertEquals(9, user.getExperience());
    assertEquals(1_500, user.getCoins());
    assertEquals(10, response.reward().experienceReward());
    assertEquals(1, response.progression().before().level());
    assertEquals(39, response.progression().before().experience());
    assertEquals(2, response.progression().after().level());
    assertEquals(9, response.progression().after().experience());
    assertEquals(50, response.progression().after().requiredExperienceForNextLevel());
  }

  @Test
  void expiresPendingCaptureAtBoundaryWithoutReward() {
    Capture capture = capture(NOW);
    User user = user();
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

    CaptureGameResultResponse response =
        service.submit(USER_ID, CAPTURE_ID, request(new ThrowResult(1, true)));

    assertEquals(GameStatus.EXPIRED, response.gameStatus());
    assertNull(response.reward());
    assertNull(response.progression().before());
    assertEquals(1, response.progression().after().level());
    assertEquals(0, response.progression().after().experience());
    assertEquals(40, response.progression().after().requiredExperienceForNextLevel());
  }

  @Test
  void usesRequestReceivedTimeWhenLockAcquisitionPassesExpiration() {
    AtomicReference<Instant> currentTime = new AtomicReference<>(NOW);
    Capture capture = capture(NOW.plusSeconds(1));
    User user = user();
    when(captureRepository.findByIdForUpdate(CAPTURE_ID))
        .thenAnswer(
            invocation -> {
              currentTime.set(NOW.plusSeconds(2));
              return Optional.of(capture);
            });
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    CaptureGameResultService delayedService =
        new CaptureGameResultService(
            captureRepository,
            userRepository,
            new CaptureGameResultPolicy(),
            new CaptureExperiencePolicy(),
            new LevelRewardPolicy(),
            Clock.tick(
                new Clock() {
                  @Override
                  public java.time.ZoneId getZone() {
                    return ZoneOffset.UTC;
                  }

                  @Override
                  public Clock withZone(java.time.ZoneId zone) {
                    return this;
                  }

                  @Override
                  public Instant instant() {
                    return currentTime.get();
                  }
                },
                java.time.Duration.ofMillis(1)));

    CaptureGameResultResponse response =
        delayedService.submit(USER_ID, CAPTURE_ID, request(new ThrowResult(1, true)));

    assertEquals(GameStatus.SUCCEEDED, response.gameStatus());
    assertEquals(10, user.getExperience());
  }

  @Test
  void returnsFirstRewardAndLatestProgressionForConflictingRetry() {
    Capture capture = capture(NOW.plusSeconds(1));
    User user = user();
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    CaptureGameResultResponse first =
        service.submit(USER_ID, CAPTURE_ID, request(new ThrowResult(1, true)));
    ReflectionTestUtils.setField(user, "level", 2);
    ReflectionTestUtils.setField(user, "experience", 5L);
    ReflectionTestUtils.setField(user, "coins", 1_500L);
    CaptureGameResultResponse retry =
        service.submit(
            USER_ID,
            CAPTURE_ID,
            request(
                new ThrowResult(1, false), new ThrowResult(2, false), new ThrowResult(3, false)));

    assertEquals(first.reward(), retry.reward());
    assertNull(retry.progression().before());
    assertEquals(2, retry.progression().after().level());
    assertEquals(5, retry.progression().after().experience());
    assertEquals(50, retry.progression().after().requiredExperienceForNextLevel());
    verify(userRepository).findByIdForUpdate(USER_ID);
    verify(userRepository).findById(USER_ID);
  }

  @Test
  void returnsFinalRewardEvenWhenRetryArrivesAfterExpiration() {
    Capture capture = capture(NOW.minusSeconds(1));
    capture.completeGame(GameStatus.SUCCEEDED, 10);
    User user = user();
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    CaptureGameResultResponse response =
        service.submit(USER_ID, CAPTURE_ID, request(new ThrowResult(1, true)));

    assertEquals(GameStatus.SUCCEEDED, response.gameStatus());
    assertEquals(10, response.reward().experienceReward());
    assertNull(response.progression().before());
    assertEquals(1, response.progression().after().level());
    verify(userRepository).findById(USER_ID);
  }

  @Test
  void rejectsAccessByAnotherUser() {
    Capture capture = capture(NOW.plusSeconds(1));
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.submit(2L, CAPTURE_ID, request(new ThrowResult(1, true))));

    assertEquals(ErrorCode.FORBIDDEN_CAPTURE_ACCESS.getCode(), exception.getCode());
  }

  private CaptureGameResultRequest request(ThrowResult... throws_) {
    return new CaptureGameResultRequest(List.of(throws_));
  }

  private User user() {
    return User.register(SocialProvider.KAKAO, "provider-id", "test@test.com");
  }

  private Capture capture(Instant expiresAt) {
    Capture capture =
        Capture.create(
            USER_ID,
            UUID.randomUUID().toString(),
            CardType.GROUND,
            Tier.C,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "123",
            "images/capture-original/1/original.jpg",
            "image/jpeg",
            expiresAt);
    ReflectionTestUtils.setField(capture, "id", CAPTURE_ID);
    return capture;
  }
}
