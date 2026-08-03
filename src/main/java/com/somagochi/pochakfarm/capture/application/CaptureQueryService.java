package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaptureQueryService {

  private final CaptureRepository captureRepository;
  private final FileStorage fileStorage;
  private final Clock clock;

  @Transactional(readOnly = true)
  public CaptureResponse getCapture(Long userId, Long captureId) {
    Capture capture =
        captureRepository
            .findById(captureId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CAPTURE_NOT_FOUND));
    if (!capture.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_CAPTURE_ACCESS);
    }
    return CaptureResponse.from(
        capture,
        capture.gameStatusAt(clock.instant()),
        buildUrlWhenSucceeded(capture, capture.getSceneImage()),
        buildUrlWhenSucceeded(capture, capture.getCardImage()),
        buildUrlWhenSucceeded(capture, capture.getAnimalImage()));
  }

  @Transactional(readOnly = true)
  public Optional<Capture> findById(Long captureId) {
    return captureRepository.findById(captureId);
  }

  private String buildUrlWhenSucceeded(Capture capture, String key) {
    if (capture.getGenerationStatus() != GenerationStatus.SUCCEEDED || key == null) {
      return null;
    }
    return fileStorage.buildUrl(key);
  }
}
