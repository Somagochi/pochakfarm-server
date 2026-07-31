package com.somagochi.pochakfarm.characterization.application;

import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardMetadataGenerator;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationStartResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CharacterizationProperties;
import java.io.IOException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CharacterizationService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final CharacterizationRepository characterizationRepository;
  private final CardMetadataGenerator cardMetadataGenerator;
  private final CharacterizationAsyncService characterizationAsyncService;
  private final CharacterizationProperties properties;

  public CharacterizationService(
      CharacterizationRepository characterizationRepository,
      CardMetadataGenerator cardMetadataGenerator,
      CharacterizationAsyncService characterizationAsyncService,
      CharacterizationProperties properties) {
    this.characterizationRepository = characterizationRepository;
    this.cardMetadataGenerator = cardMetadataGenerator;
    this.characterizationAsyncService = characterizationAsyncService;
    this.properties = properties;
  }

  public CharacterizationStartResponse characterize(
      Long deviceId, MultipartFile image, String animalName) {
    validateImage(image);
    AnimalName normalizedAnimalName = AnimalName.from(animalName);
    if (properties.deviceLimitEnabled()) {
      validateDeviceCanCharacterize(deviceId);
    }
    byte[] sourceImage = readImageBytes(image);
    CardMetadata metadata = cardMetadataGenerator.generate();

    Characterization characterization =
        save(Characterization.start(deviceId, normalizedAnimalName, metadata));
    metadata = metadata.withCardNo(formatCardNo(characterization.getId()));
    characterization.cardNoAssigned(metadata.cardNo());
    save(characterization);
    try {
      characterizationAsyncService.characterizeAsync(
          characterization.getId(),
          sourceImage,
          image.getContentType(),
          normalizedAnimalName.value(),
          metadata);
    } catch (TaskRejectedException exception) {
      characterization.fail(ErrorCode.CHARACTERIZATION_BUSY.getCode());
      save(characterization);
      log.warn(
          "characterization_async_rejected id={} deviceId={}",
          characterization.getId(),
          deviceId,
          exception);
      throw new BusinessException(ErrorCode.CHARACTERIZATION_BUSY);
    }
    return new CharacterizationStartResponse(
        characterization.getId(), characterization.getStatus(), characterization.getCardType());
  }

  private Characterization save(Characterization characterization) {
    Characterization saved = characterizationRepository.save(characterization);
    return saved == null ? characterization : saved;
  }

  private String formatCardNo(Long characterizationId) {
    long source = characterizationId == null ? 1L : characterizationId;
    long displayNumber = Math.max(source, 1L) % 1000L;
    return "%03d".formatted(displayNumber);
  }

  private byte[] readImageBytes(MultipartFile image) {
    try {
      return image.getBytes();
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
  }

  private void validateDeviceCanCharacterize(Long deviceId) {
    if (characterizationRepository.existsByDeviceIdAndStatus(
        deviceId, CharacterizationStatus.SUCCEEDED)) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_ALREADY_USED);
    }
    if (characterizationRepository.existsByDeviceIdAndStatus(
        deviceId, CharacterizationStatus.PROCESSING)) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_ALREADY_PROCESSING);
    }
  }

  private void validateImage(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new BusinessException(ErrorCode.EMPTY_FILE);
    }
    if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }
  }
}
