package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.capture.domain.DailyCaptureAttempt;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.DailyCaptureAttemptRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaptureAvailabilityServiceTest {

  @Mock private DailyCaptureAttemptRepository attemptRepository;
  @Mock private UserRepository userRepository;
  private CaptureAvailabilityService service;

  @BeforeEach
  void setUp() {
    service =
        new CaptureAvailabilityService(
            attemptRepository,
            userRepository,
            Clock.fixed(Instant.parse("2026-08-02T01:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void returnsPersistedAttemptBalance() {
    User user = User.register(SocialProvider.KAKAO, "provider", "user@test.com");
    given(userRepository.findById(1L)).willReturn(Optional.of(user));
    given(attemptRepository.findByUserIdAndAttemptDate(1L, LocalDate.of(2026, 8, 2)))
        .willReturn(Optional.of(DailyCaptureAttempt.create(1L, LocalDate.of(2026, 8, 2), 2)));

    CaptureAvailabilityResponse response = service.getAvailability(1L);

    assertEquals(2, response.attempts().remaining());
    assertEquals(Instant.parse("2026-08-02T15:00:00Z"), response.attempts().resetsAt());
    assertEquals(200, response.attemptPurchaseCost());
    assertEquals(1000, response.coins());
  }

  @Test
  void returnsFiveForNewKoreaDateWithoutWriting() {
    given(userRepository.findById(1L))
        .willReturn(Optional.of(User.register(SocialProvider.KAKAO, "provider", "user@test.com")));
    given(attemptRepository.findByUserIdAndAttemptDate(1L, LocalDate.of(2026, 8, 2)))
        .willReturn(Optional.empty());

    assertEquals(5, service.getAvailability(1L).attempts().remaining());
  }

  @Test
  void rejectsUnknownUser() {
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.getAvailability(1L));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }
}
