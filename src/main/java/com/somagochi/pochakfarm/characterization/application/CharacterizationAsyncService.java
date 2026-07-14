package com.somagochi.pochakfarm.characterization.application;

import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CharacterizationAsyncService {

  private static final String RESULT_PURPOSE = "characterization-result";

  private final CharacterizationRepository characterizationRepository;
  private final CharacterizerClient characterizerClient;
  private final ImageUploadService imageUploadService;

  public CharacterizationAsyncService(
      CharacterizationRepository characterizationRepository,
      CharacterizerClient characterizerClient,
      ImageUploadService imageUploadService) {
    this.characterizationRepository = characterizationRepository;
    this.characterizerClient = characterizerClient;
    this.imageUploadService = imageUploadService;
  }

  @Async
  @Transactional
  public void characterizeAsync(
      Long characterizationId,
      byte[] sourceImage,
      String sourceImageContentType,
      String animalName,
      CardMetadata metadata) {
    Characterization characterization =
        characterizationRepository
            .findById(characterizationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTERIZATION_NOT_FOUND));
    try {
      long characterizerStartedAt = System.nanoTime();
      CharacterizerResult result =
          characterizerClient.characterize(
              encodeSourceImage(sourceImage), sourceImageContentType, animalName, metadata);
      log.info(
          "characterization_python_completed id={} provider={} pythonElapsedMs={} clientElapsedMs={}",
          characterizationId,
          result.provider(),
          result.elapsedMs(),
          elapsedMsSince(characterizerStartedAt));

      long resultUploadStartedAt = System.nanoTime();
      byte[] resultImage = decodeCardImage(result);
      PublicUploadResponse resultUpload =
          imageUploadService.uploadPublic(RESULT_PURPOSE, result.contentType(), resultImage);
      log.info(
          "characterization_result_uploaded id={} key={} bytes={} elapsedMs={}",
          characterizationId,
          resultUpload.key(),
          resultImage.length,
          elapsedMsSince(resultUploadStartedAt));

      characterization.succeed(resultUpload.key(), result.provider(), result.elapsedMs());
    } catch (BusinessException exception) {
      characterization.fail(exception.getCode());
      log.warn(
          "characterization_async_failed id={} errorCode={}",
          characterizationId,
          exception.getCode());
    } catch (RuntimeException exception) {
      characterization.fail(ErrorCode.CHARACTERIZATION_FAILED.getCode());
      log.warn(
          "characterization_async_failed id={} errorCode={} exception={}",
          characterizationId,
          ErrorCode.CHARACTERIZATION_FAILED.getCode(),
          exception.getClass().getSimpleName());
    }
  }

  private String encodeSourceImage(byte[] sourceImage) {
    return Base64.getEncoder().encodeToString(sourceImage);
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

  private long elapsedMsSince(long startedAtNanos) {
    return (System.nanoTime() - startedAtNanos) / 1_000_000;
  }
}
