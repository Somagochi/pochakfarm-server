package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficulty;
import com.somagochi.pochakfarm.capture.domain.CaptureDifficultyPolicy;
import com.somagochi.pochakfarm.capture.domain.CardTypeSelectionPolicy;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.domain.TierSelectionPolicy;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardMetadataGenerator;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaptureStartService {

  private static final int DAILY_LIMIT = 5;
  private static final Duration GAME_RESULT_SUBMISSION_WINDOW = Duration.ofMinutes(5);
  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final String ORIGINAL_IMAGE_PURPOSE = "capture-original";
  private static final String TEMPORARY_CARD_NO = "001";

  private final CaptureRepository captureRepository;
  private final UserRepository userRepository;
  private final TierSelectionPolicy tierSelectionPolicy;
  private final CardTypeSelectionPolicy cardTypeSelectionPolicy;
  private final CaptureDifficultyPolicy captureDifficultyPolicy;
  private final CardMetadataGenerator cardMetadataGenerator;
  private final ImageUploadService imageUploadService;
  private final Clock clock;

  public CaptureStartService(
      CaptureRepository captureRepository,
      UserRepository userRepository,
      TierSelectionPolicy tierSelectionPolicy,
      CardTypeSelectionPolicy cardTypeSelectionPolicy,
      CaptureDifficultyPolicy captureDifficultyPolicy,
      CardMetadataGenerator cardMetadataGenerator,
      ImageUploadService imageUploadService,
      Clock clock) {
    this.captureRepository = captureRepository;
    this.userRepository = userRepository;
    this.tierSelectionPolicy = tierSelectionPolicy;
    this.cardTypeSelectionPolicy = cardTypeSelectionPolicy;
    this.captureDifficultyPolicy = captureDifficultyPolicy;
    this.cardMetadataGenerator = cardMetadataGenerator;
    this.imageUploadService = imageUploadService;
    this.clock = clock;
  }

  @Transactional
  public CaptureStartResponse startCapture(Long userId, CaptureStartRequest request) {
    String clientRequestId = normalizeClientRequestId(request.clientRequestId());
    AnimalName animalName = AnimalName.from(request.animalName());
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    DailyRange dailyRange = currentDailyRange();
    Optional<Capture> existing =
        captureRepository.findByUserIdAndClientRequestId(userId, clientRequestId);
    if (existing.isPresent()) {
      return existingResponse(existing.get(), request.contentType(), animalName, dailyRange);
    }

    long used = countUsed(userId, dailyRange);
    if (used >= DAILY_LIMIT) {
      throw new BusinessException(ErrorCode.CAPTURE_ATTEMPT_EXHAUSTED);
    }

    Tier tier = tierSelectionPolicy.select(user.getLevel());
    CardType cardType = cardTypeSelectionPolicy.select();
    CardMetadata metadata = cardMetadataGenerator.generate(cardType);
    CaptureDifficulty difficulty = captureDifficultyPolicy.forTier(tier);
    PresignResponse presign =
        imageUploadService.createPresign(userId, ORIGINAL_IMAGE_PURPOSE, request.contentType());
    Instant gameResultExpiresAt = clock.instant().plus(GAME_RESULT_SUBMISSION_WINDOW);
    Capture capture =
        Capture.create(
            userId,
            clientRequestId,
            cardType,
            tier,
            animalName,
            metadata.skill1(),
            metadata.skill2(),
            TEMPORARY_CARD_NO,
            presign.key(),
            request.contentType(),
            gameResultExpiresAt);
    Capture saved = captureRepository.save(capture);
    saved.cardNoAssigned(formatCardNo(saved.getId()));
    return CaptureStartResponse.from(saved, difficulty, presign, DAILY_LIMIT, used + 1);
  }

  private CaptureStartResponse existingResponse(
      Capture capture, String requestedContentType, AnimalName animalName, DailyRange dailyRange) {
    if (!capture.getOriginalImageContentType().equals(requestedContentType)
        || !capture.getAnimalName().equals(animalName.value())) {
      throw new BusinessException(ErrorCode.CAPTURE_REQUEST_CONFLICT);
    }
    PresignResponse presign =
        imageUploadService.refreshPresign(
            capture.getUserId(),
            capture.getOriginalImageKey(),
            capture.getOriginalImageContentType());
    long used = countUsed(capture.getUserId(), dailyRange);
    return CaptureStartResponse.from(
        capture, captureDifficultyPolicy.forTier(capture.getTier()), presign, DAILY_LIMIT, used);
  }

  private long countUsed(Long userId, DailyRange range) {
    return captureRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        userId, range.startInclusive(), range.endExclusive());
  }

  private DailyRange currentDailyRange() {
    LocalDate today = LocalDate.now(clock.withZone(KOREA_ZONE));
    return new DailyRange(
        today.atStartOfDay(KOREA_ZONE).toInstant(),
        today.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant());
  }

  private String normalizeClientRequestId(String clientRequestId) {
    try {
      UUID parsed = UUID.fromString(clientRequestId);
      if (!parsed.toString().equalsIgnoreCase(clientRequestId)) {
        throw new IllegalArgumentException();
      }
      return parsed.toString();
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BusinessException(ErrorCode.INVALID_CLIENT_REQUEST_ID);
    }
  }

  private String formatCardNo(Long captureId) {
    long source = captureId == null ? 1L : captureId;
    long displayNumber = Math.max(source, 1L) % 1000L;
    return "%03d".formatted(displayNumber);
  }

  private record DailyRange(Instant startInclusive, Instant endExclusive) {}
}
