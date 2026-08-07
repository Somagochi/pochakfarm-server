package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaptureGrantService {

  private static final String CARD_IMAGE_CONTENT_TYPE = "image/png";

  private final CaptureRepository captureRepository;
  private final FileStorage fileStorage;

  @Transactional
  public Capture grant(Long userId, Characterization characterization, Tier tier, Instant now) {
    return captureRepository.save(
        Capture.granted(
            userId,
            characterization.getCardType(),
            tier,
            AnimalName.from(characterization.getAnimalName()),
            characterization.getSkill1(),
            characterization.getSkill2(),
            characterization.getCardNo(),
            characterization.getResultImageKey(),
            CARD_IMAGE_CONTENT_TYPE,
            now));
  }

  @Transactional
  public Capture completeGrant(Long userId, Long captureId, String sourceAnimalImageKey) {
    Capture capture =
        captureRepository
            .findById(captureId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CAPTURE_NOT_FOUND));
    if (!capture.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_CAPTURE_ACCESS);
    }
    if (sourceAnimalImageKey == null || sourceAnimalImageKey.isBlank()) {
      throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }
    String animalImageKey = CaptureAnimalImageKeys.of(userId, captureId);
    fileStorage.copy(sourceAnimalImageKey, animalImageKey);
    capture.completeGrant(animalImageKey);
    return capture;
  }

  @Transactional(readOnly = true)
  public Capture getById(Long captureId) {
    return captureRepository
        .findById(captureId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CAPTURE_NOT_FOUND));
  }
}
