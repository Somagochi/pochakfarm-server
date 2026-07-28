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
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.domain.TierSelectionPolicy;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
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
  private static final String CLIENT_REQUEST_ID = "550e8400-e29b-41d4-a716-446655440000";
  private static final String CONTENT_TYPE = "image/jpeg";
  private static final String ORIGINAL_IMAGE = "images/capture-original/1/original.jpg";
  private static final Instant NOW = Instant.parse("2026-07-24T01:00:00Z");

  @Mock private CaptureRepository captureRepository;
  @Mock private UserRepository userRepository;
  @Mock private TierSelectionPolicy tierSelectionPolicy;
  @Mock private CardTypeSelectionPolicy cardTypeSelectionPolicy;
  @Mock private CaptureDifficultyPolicy captureDifficultyPolicy;
  @Mock private ImageUploadService imageUploadService;

  private CaptureStartService service;

  @BeforeEach
  void setUp() {
    service =
        new CaptureStartService(
            captureRepository,
            userRepository,
            tierSelectionPolicy,
            cardTypeSelectionPolicy,
            captureDifficultyPolicy,
            imageUploadService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void startsCaptureAndConsumesOneDailyAttempt() {
    User user = user();
    CaptureDifficulty difficulty = new CaptureDifficulty(10_000, 3, 2_800, 280);
    PresignResponse presign =
        new PresignResponse(
            "https://upload.example/original", ORIGINAL_IMAGE, NOW.plusSeconds(300));
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, CLIENT_REQUEST_ID))
        .willReturn(Optional.empty());
    given(
            captureRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                USER_ID,
                Instant.parse("2026-07-23T15:00:00Z"),
                Instant.parse("2026-07-24T15:00:00Z")))
        .willReturn(0L);
    given(tierSelectionPolicy.select(1)).willReturn(Tier.B);
    given(cardTypeSelectionPolicy.select()).willReturn(CardType.GROUND);
    given(captureDifficultyPolicy.forTier(Tier.B)).willReturn(difficulty);
    given(imageUploadService.createPresign(USER_ID, "capture-original", CONTENT_TYPE))
        .willReturn(presign);
    given(captureRepository.save(any(Capture.class)))
        .willAnswer(
            invocation -> {
              Capture capture = invocation.getArgument(0);
              ReflectionTestUtils.setField(capture, "id", 123L);
              return capture;
            });

    CaptureStartResponse response =
        service.startCapture(USER_ID, new CaptureStartRequest(CLIENT_REQUEST_ID, CONTENT_TYPE));

    assertEquals(123L, response.captureId());
    assertEquals(Tier.B, response.tier());
    assertEquals(CardType.GROUND, response.cardType());
    assertEquals(difficulty, response.difficulty());
    assertEquals("https://upload.example/original", response.upload().url());
    assertEquals(ORIGINAL_IMAGE, response.upload().key());
    assertEquals(5, response.attempts().dailyLimit());
    assertEquals(1, response.attempts().used());
    assertEquals(4, response.attempts().remaining());
    assertEquals(NOW.plusSeconds(300), response.gameResultExpiresAt());

    verify(captureRepository).save(any(Capture.class));
  }

  @Test
  void returnsExistingCaptureWithoutConsumingAgainForSameClientRequest() {
    User user = user();
    Capture existing =
        Capture.create(
            USER_ID,
            CLIENT_REQUEST_ID,
            CardType.SKY,
            Tier.A,
            ORIGINAL_IMAGE,
            CONTENT_TYPE,
            NOW.plusSeconds(300));
    ReflectionTestUtils.setField(existing, "id", 55L);
    CaptureDifficulty difficulty = new CaptureDifficulty(10_000, 3, 2_400, 240);
    PresignResponse presign =
        new PresignResponse(
            "https://upload.example/refreshed", ORIGINAL_IMAGE, NOW.plusSeconds(300));
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, CLIENT_REQUEST_ID))
        .willReturn(Optional.of(existing));
    given(
            captureRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(), any(), any()))
        .willReturn(5L);
    given(captureDifficultyPolicy.forTier(Tier.A)).willReturn(difficulty);
    given(imageUploadService.refreshPresign(USER_ID, ORIGINAL_IMAGE, CONTENT_TYPE))
        .willReturn(presign);

    CaptureStartResponse response =
        service.startCapture(USER_ID, new CaptureStartRequest(CLIENT_REQUEST_ID, CONTENT_TYPE));

    assertEquals(55L, response.captureId());
    assertEquals(Tier.A, response.tier());
    assertEquals("https://upload.example/refreshed", response.upload().url());
    assertEquals(5, response.attempts().used());
    assertEquals(0, response.attempts().remaining());
    verify(tierSelectionPolicy, never()).select(any(Integer.class));
    verify(cardTypeSelectionPolicy, never()).select();
    verify(captureRepository, never()).save(any());
    verify(imageUploadService, never()).createPresign(any(), any(), any());
  }

  @Test
  void rejectsSameClientRequestIdWithDifferentContentType() {
    Capture existing =
        Capture.create(
            USER_ID,
            CLIENT_REQUEST_ID,
            CardType.SKY,
            Tier.A,
            ORIGINAL_IMAGE,
            CONTENT_TYPE,
            NOW.plusSeconds(300));
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, CLIENT_REQUEST_ID))
        .willReturn(Optional.of(existing));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(
                    USER_ID, new CaptureStartRequest(CLIENT_REQUEST_ID, "image/png")));

    assertEquals(ErrorCode.CAPTURE_REQUEST_CONFLICT.getCode(), exception.getCode());
    verify(imageUploadService, never()).refreshPresign(any(), any(), any());
  }

  @Test
  void rejectsCaptureWhenFiveDailyAttemptsAreUsed() {
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user()));
    given(captureRepository.findByUserIdAndClientRequestId(USER_ID, CLIENT_REQUEST_ID))
        .willReturn(Optional.empty());
    given(
            captureRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(), any(), any()))
        .willReturn(5L);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(
                    USER_ID, new CaptureStartRequest(CLIENT_REQUEST_ID, CONTENT_TYPE)));

    assertEquals(ErrorCode.CAPTURE_ATTEMPT_EXHAUSTED.getCode(), exception.getCode());
    verify(captureRepository, never()).save(any());
    verify(imageUploadService, never()).createPresign(any(), any(), any());
  }

  @Test
  void rejectsUnknownUser() {
    given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(
                    USER_ID, new CaptureStartRequest(CLIENT_REQUEST_ID, CONTENT_TYPE)));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void rejectsInvalidClientRequestId() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(USER_ID, new CaptureStartRequest("not-a-uuid", CONTENT_TYPE)));

    assertEquals(ErrorCode.INVALID_CLIENT_REQUEST_ID.getCode(), exception.getCode());
    verify(userRepository, never()).findByIdForUpdate(any());
  }

  @Test
  void rejectsNonCanonicalClientRequestIdAcceptedByJavaUuidParser() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.startCapture(USER_ID, new CaptureStartRequest("1-1-1-1-1", CONTENT_TYPE)));

    assertEquals(ErrorCode.INVALID_CLIENT_REQUEST_ID.getCode(), exception.getCode());
    verify(userRepository, never()).findByIdForUpdate(any());
  }

  private User user() {
    return User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
  }
}
