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
  private static final String ORIGINAL_PURPOSE = "characterization-original";
  private static final String RESULT_PURPOSE = "characterization-result";

  private final CharacterizationRepository characterizationRepository;
  private final CharacterizerClient characterizerClient;
  private final CardMetadataGenerator cardMetadataGenerator;
  private final ImageUploadService imageUploadService;

  public CharacterizationService(
      CharacterizationRepository characterizationRepository,
      CharacterizerClient characterizerClient,
      CardMetadataGenerator cardMetadataGenerator,
      ImageUploadService imageUploadService) {
    this.characterizationRepository = characterizationRepository;
    this.characterizerClient = characterizerClient;
    this.cardMetadataGenerator = cardMetadataGenerator;
    this.imageUploadService = imageUploadService;
  }

  public CharacterizationResponse characterize(
      Long deviceId, MultipartFile image, String animalName) {
    validateImage(image);
    String normalizedAnimalName = normalizeAnimalName(animalName);
    validateDeviceCanCharacterize(deviceId);
    CardMetadata metadata = cardMetadataGenerator.generate();

    Characterization characterization =
        save(Characterization.start(deviceId, normalizedAnimalName, metadata));
    metadata = metadata.withCardNo(formatCardNo(characterization.getId()));
    characterization.cardNoAssigned(metadata.cardNo());
    try {
      long originalUploadStartedAt = System.nanoTime();
      PublicUploadResponse original = uploadOriginal(image);
      log.info(
          "characterization_original_uploaded key={} elapsedMs={}",
          original.key(),
          elapsedMsSince(originalUploadStartedAt));
      characterization.originalUploaded(original.key());

      long characterizerStartedAt = System.nanoTime();
      CharacterizerResult result =
          characterizerClient.characterize(image, normalizedAnimalName, metadata);
      log.info(
          "characterization_python_completed provider={} fallbackFrom={} pythonElapsedMs={} clientElapsedMs={}",
          result.provider(),
          result.fallbackFrom(),
          result.elapsedMs(),
          elapsedMsSince(characterizerStartedAt));

      long resultUploadStartedAt = System.nanoTime();
      byte[] resultImage = decodeResultImage(result);
      PublicUploadResponse resultUpload =
          imageUploadService.uploadPublic(RESULT_PURPOSE, result.contentType(), resultImage);
      log.info(
          "characterization_result_uploaded key={} bytes={} elapsedMs={}",
          resultUpload.key(),
          resultImage.length,
          elapsedMsSince(resultUploadStartedAt));

      characterization.succeed(resultUpload.key(), result.provider(), result.elapsedMs());
      save(characterization);
      return new CharacterizationResponse(
          "success",
          result.provider(),
          result.animalName(),
          metadata.cardTypeLabel(),
          metadata.power(),
          metadata.skill1().displayName(),
          metadata.skill1().description(),
          metadata.skill2().displayName(),
          metadata.skill2().description(),
          metadata.cardNo(),
          resultUpload.url(),
          result.elapsedMs());
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
    return "No.%03d".formatted(displayNumber);
  }

  private long elapsedMsSince(long startedAtNanos) {
    return (System.nanoTime() - startedAtNanos) / 1_000_000;
  }

  private PublicUploadResponse uploadOriginal(MultipartFile image) {
    try {
      return imageUploadService.uploadPublic(
          ORIGINAL_PURPOSE, image.getContentType(), image.getBytes());
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
  }

  private byte[] decodeResultImage(CharacterizerResult result) {
    if (result == null
        || !"success".equals(result.status())
        || result.imageBase64() == null
        || result.contentType() == null) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
    try {
      return Base64.getDecoder().decode(result.imageBase64());
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
    return animalName.trim();
  }
}
