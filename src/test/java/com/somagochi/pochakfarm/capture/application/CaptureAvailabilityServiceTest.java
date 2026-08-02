package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CaptureAvailabilityServiceTest {

  private static final Long USER_ID = 1L;
  private static final Instant NOW = Instant.parse("2026-08-02T01:00:00Z");

  @Mock private CaptureRepository captureRepository;
  @Mock private UserRepository userRepository;

  private CaptureAvailabilityService service;

  @BeforeEach
  void setUp() {
    service =
        new CaptureAvailabilityService(
            captureRepository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void returnsFreeAttemptAndCoinAvailabilityForCurrentUser() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(
            captureRepository.countFreeAttemptsByUserIdBetween(
                USER_ID,
                Instant.parse("2026-08-01T15:00:00Z"),
                Instant.parse("2026-08-02T15:00:00Z")))
        .willReturn(3L);

    CaptureAvailabilityResponse response = service.getAvailability(USER_ID);

    assertEquals(5, response.freeAttempts().dailyLimit());
    assertEquals(3, response.freeAttempts().used());
    assertEquals(2, response.freeAttempts().remaining());
    assertEquals(Instant.parse("2026-08-02T15:00:00Z"), response.freeAttempts().resetsAt());
    assertEquals(200, response.extraCaptureCost());
    assertEquals(1000, response.coins());
    assertTrue(response.canStartCapture());
  }

  @Test
  void canStartCaptureWhenFreeAttemptsAreUsedButCoinsAreEnough() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(
            captureRepository.countFreeAttemptsByUserIdBetween(
                USER_ID,
                Instant.parse("2026-08-01T15:00:00Z"),
                Instant.parse("2026-08-02T15:00:00Z")))
        .willReturn(5L);

    CaptureAvailabilityResponse response = service.getAvailability(USER_ID);

    assertEquals(0, response.freeAttempts().remaining());
    assertTrue(response.canStartCapture());
  }

  @Test
  void cannotStartCaptureWhenFreeAttemptsAreUsedAndCoinsAreInsufficient() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
    ReflectionTestUtils.setField(user, "coins", 100L);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(
            captureRepository.countFreeAttemptsByUserIdBetween(
                USER_ID,
                Instant.parse("2026-08-01T15:00:00Z"),
                Instant.parse("2026-08-02T15:00:00Z")))
        .willReturn(5L);

    CaptureAvailabilityResponse response = service.getAvailability(USER_ID);

    assertEquals(0, response.freeAttempts().remaining());
    assertEquals(100, response.coins());
    assertFalse(response.canStartCapture());
  }

  @Test
  void rejectsUnknownUser() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.getAvailability(USER_ID));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }
}
