package com.somagochi.pochakfarm.characterization.application;

import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardMetadataGenerator;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CharacterizationProperties;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CharacterizationService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final int MAX_ANIMAL_NAME_LENGTH = 6;
  private static final String RESULT_PURPOSE = "characterization-result";
  private static final String BACK_PURPOSE = "characterization-back";

  private final CharacterizationRepository characterizationRepository;
  private final CharacterizerClient characterizerClient;
  private final CardMetadataGenerator cardMetadataGenerator;
  private final ImageUploadService imageUploadService;
  private final CharacterizationProperties properties;

  public CharacterizationService(
      CharacterizationRepository characterizationRepository,
      CharacterizerClient characterizerClient,
      CardMetadataGenerator cardMetadataGenerator,
      ImageUploadService imageUploadService,
      CharacterizationProperties properties) {
    this.characterizationRepository = characterizationRepository;
    this.characterizerClient = characterizerClient;
    this.cardMetadataGenerator = cardMetadataGenerator;
    this.imageUploadService = imageUploadService;
    this.properties = properties;
  }

  public CharacterizationResponse characterize(
      Long deviceId, MultipartFile image, String animalName) {
    validateImage(image);
    String normalizedAnimalName = normalizeAnimalName(animalName);
    if (properties.deviceLimitEnabled()) {
      validateDeviceCanCharacterize(deviceId);
    }
    CardMetadata metadata = cardMetadataGenerator.generate();

    Characterization characterization =
        save(Characterization.start(deviceId, normalizedAnimalName, metadata));
    metadata = metadata.withCardNo(formatCardNo(characterization.getId()));
    characterization.cardNoAssigned(metadata.cardNo());
    try {
      long characterizerStartedAt = System.nanoTime();
      CharacterizerResult result =
          characterizerClient.characterize(
              encodeSourceImage(image), image.getContentType(), normalizedAnimalName, metadata);
      log.info(
          "characterization_python_completed provider={} pythonElapsedMs={} clientElapsedMs={}",
          result.provider(),
          result.elapsedMs(),
          elapsedMsSince(characterizerStartedAt));

      long resultUploadStartedAt = System.nanoTime();
      byte[] resultImage = decodeCardImage(result);
      PublicUploadResponse resultUpload =
          imageUploadService.uploadPublic(RESULT_PURPOSE, result.contentType(), resultImage);
      log.info(
          "characterization_result_uploaded key={} bytes={} elapsedMs={}",
          resultUpload.key(),
          resultImage.length,
          elapsedMsSince(resultUploadStartedAt));

      long backUploadStartedAt = System.nanoTime();
      byte[] backImage = decodeCardBackImage(result);
      PublicUploadResponse backUpload =
          imageUploadService.uploadPublic(BACK_PURPOSE, result.contentType(), backImage);
      log.info(
          "characterization_back_uploaded key={} bytes={} elapsedMs={}",
          backUpload.key(),
          backImage.length,
          elapsedMsSince(backUploadStartedAt));

      characterization.succeed(
          resultUpload.key(), backUpload.key(), result.provider(), result.elapsedMs());
      save(characterization);
      return new CharacterizationResponse(resultUpload.url(), backUpload.url());
    } catch (BusinessException exception) {
      characterization.fail(exception.getCode());
      save(characterization);
      throw exception;
    } catch (RuntimeException exception) {
      characterization.fail(ErrorCode.CHARACTERIZATION_FAILED.getCode());
      save(characterization);
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
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

  private long elapsedMsSince(long startedAtNanos) {
    return (System.nanoTime() - startedAtNanos) / 1_000_000;
  }

  private String encodeSourceImage(MultipartFile image) {
    try {
      return Base64.getEncoder().encodeToString(image.getBytes());
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
  }

  private byte[] decodeCardImage(CharacterizerResult result) {
    if (result == null
        || !"success".equals(result.status())
        || result.cardImageBase64() == null
        || result.contentType() == null) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
    return decodeBase64Image(result.cardImageBase64());
  }

  private byte[] decodeCardBackImage(CharacterizerResult result) {
    if (result == null
        || !"success".equals(result.status())
        || result.cardBackImageBase64() == null
        || result.contentType() == null) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
    return decodeBase64Image(result.cardBackImageBase64());
  }

  private byte[] decodeBase64Image(String imageBase64) {
    if (imageBase64 == null || imageBase64.isBlank()) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
    try {
      return Base64.getDecoder().decode(imageBase64);
    } catch (IllegalArgumentException exception) {
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

  private String normalizeAnimalName(String animalName) {
    if (animalName == null || animalName.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_ANIMAL_NAME);
    }
    String normalized = animalName.trim();
    if (normalized.length() > MAX_ANIMAL_NAME_LENGTH) {
      throw new BusinessException(ErrorCode.INVALID_ANIMAL_NAME);
    }
    return normalized;
  }
}
