package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficulty;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficultyPolicy;
import com.somagochi.pochakfarm.capture.domain.CardTypeSelectionPolicy;
import com.somagochi.pochakfarm.capture.domain.DailyCaptureAttempt;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.domain.TierSelectionPolicy;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.DailyCaptureAttemptRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardMetadataGenerator;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CaptureStartServiceTest {

  private static final Long USER_ID = 1L;
  private static final String REQUEST_ID = "550e8400-e29b-41d4-a716-446655440000";
  private static final Instant NOW = Instant.parse("2026-07-24T01:00:00Z");

  @Mock private CaptureRepository captureRepository;
  @Mock private DailyCaptureAttemptRepository attemptRepository;
  @Mock private UserRepository userRepository;
  @Mock private TierSelectionPolicy tierSelectionPolicy;
  @Mock private CardTypeSelectionPolicy cardTypeSelectionPolicy;
  @Mock private CaptureDifficultyPolicy difficultyPolicy;
  @Mock private CardMetadataGenerator metadataGenerator;
  @Mock private ImageUploadService imageUploadService;
  private CaptureStartService service;

  @BeforeEach
  void setUp() {
    service =
        new CaptureStartService(
            captureRepository,
            attemptRepository,
            userRepository,
            tierSelectionPolicy,
            cardTypeSelectionPolicy,
            difficultyPolicy,
            metadataGenerator,
            imageUploadService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void startsCaptureAndConsumesOneAttemptWithoutSpendingCoins() {
    User user = User.register(SocialProvider.KAKAO, "provider", "user@test.com");
    DailyCaptureAttempt attempt = DailyCaptureAttempt.create(USER_ID, LocalDate.of(2026, 7, 24), 5);
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, REQUEST_ID))
        .willReturn(Optional.empty());
    given(attemptRepository.findByUserIdAndAttemptDateForUpdate(USER_ID, LocalDate.of(2026, 7, 24)))
        .willReturn(Optional.of(attempt));
    given(tierSelectionPolicy.select(1)).willReturn(Tier.B);
    given(cardTypeSelectionPolicy.select()).willReturn(CardType.GROUND);
    given(metadataGenerator.generate(CardType.GROUND))
        .willReturn(
            new CardMetadata(
                CardType.GROUND,
                82,
                CardSkill.GROUND_PAW_STRIKE,
                CardSkill.GROUND_LEAF_GUARD,
                "001"));
    CaptureDifficulty difficulty = new CaptureDifficulty(2800);
    given(difficultyPolicy.forTier(Tier.B)).willReturn(difficulty);
    given(imageUploadService.createPresign(USER_ID, "capture-original", "image/jpeg"))
        .willReturn(new PresignResponse("https://upload", "original.jpg", NOW.plusSeconds(300)));
    given(captureRepository.save(any(Capture.class)))
        .willAnswer(
            invocation -> {
              Capture capture = invocation.getArgument(0);
              ReflectionTestUtils.setField(capture, "id", 123L);
              return capture;
            });

    CaptureStartResponse response =
        service.startCapture(USER_ID, new CaptureStartRequest(REQUEST_ID, "image/jpeg", "두부"));

    assertEquals(4, attempt.getRemaining());
    assertEquals(4, response.attempts().remaining());
    assertEquals(1000, user.getCoins());
  }

  @Test
  void rejectsStartWhenNoAttemptRemains() {
    given(userRepository.findByIdForUpdate(USER_ID))
        .willReturn(Optional.of(User.register(SocialProvider.KAKAO, "provider", "user@test.com")));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, REQUEST_ID))
        .willReturn(Optional.empty());
    given(attemptRepository.findByUserIdAndAttemptDateForUpdate(USER_ID, LocalDate.of(2026, 7, 24)))
        .willReturn(Optional.of(DailyCaptureAttempt.create(USER_ID, LocalDate.of(2026, 7, 24), 0)));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(
                    USER_ID, new CaptureStartRequest(REQUEST_ID, "image/jpeg", "두부")));

    assertEquals(ErrorCode.CAPTURE_ATTEMPT_REQUIRED.getCode(), exception.getCode());
    verify(captureRepository, never()).save(any());
  }

  @Test
  void returnsExistingCaptureWithoutConsumingAgain() {
    User user = User.register(SocialProvider.KAKAO, "provider", "user@test.com");
    Capture capture =
        Capture.create(
            USER_ID,
            REQUEST_ID,
            CardType.SKY,
            Tier.A,
            AnimalName.from("두부"),
            CardSkill.SKY_CLOUD_JUMP,
            CardSkill.SKY_WIND_DASH,
            "055",
            "original.jpg",
            "image/jpeg",
            NOW.plusSeconds(300));
    ReflectionTestUtils.setField(capture, "id", 55L);
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, REQUEST_ID))
        .willReturn(Optional.of(capture));
    given(attemptRepository.findByUserIdAndAttemptDate(USER_ID, LocalDate.of(2026, 7, 24)))
        .willReturn(Optional.of(DailyCaptureAttempt.create(USER_ID, LocalDate.of(2026, 7, 24), 2)));
    given(difficultyPolicy.forTier(Tier.A)).willReturn(new CaptureDifficulty(2400));
    given(imageUploadService.refreshPresign(USER_ID, "original.jpg", "image/jpeg"))
        .willReturn(new PresignResponse("https://refresh", "original.jpg", NOW.plusSeconds(300)));

    CaptureStartResponse response =
        service.startCapture(USER_ID, new CaptureStartRequest(REQUEST_ID, "image/jpeg", "두부"));

    assertEquals(55L, response.captureId());
    assertEquals(2, response.attempts().remaining());
    verify(attemptRepository, never()).findByUserIdAndAttemptDateForUpdate(any(), any());
  }

  @Test
  void rejectsConflictingRetryWithoutConsumingAttempt() {
    Capture capture =
        Capture.create(
            USER_ID,
            REQUEST_ID,
            CardType.SKY,
            Tier.A,
            AnimalName.from("두부"),
            CardSkill.SKY_CLOUD_JUMP,
            CardSkill.SKY_WIND_DASH,
            "055",
            "original.jpg",
            "image/jpeg",
            NOW.plusSeconds(300));
    given(userRepository.findByIdForUpdate(USER_ID))
        .willReturn(Optional.of(User.register(SocialProvider.KAKAO, "provider", "user@test.com")));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, REQUEST_ID))
        .willReturn(Optional.of(capture));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(
                    USER_ID, new CaptureStartRequest(REQUEST_ID, "image/png", "두부")));

    assertEquals(ErrorCode.CAPTURE_REQUEST_CONFLICT.getCode(), exception.getCode());
    verify(attemptRepository, never()).findByUserIdAndAttemptDateForUpdate(any(), any());
  }

  @Test
  void rejectsInvalidClientRequestIdBeforeLockingUser() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(
                    USER_ID, new CaptureStartRequest("not-a-uuid", "image/jpeg", "두부")));

    assertEquals(ErrorCode.INVALID_CLIENT_REQUEST_ID.getCode(), exception.getCode());
    verify(userRepository, never()).findByIdForUpdate(any());
  }

  @Test
  void rejectsUnknownUser() {
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(
                    USER_ID, new CaptureStartRequest(REQUEST_ID, "image/jpeg", "두부")));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }
}
