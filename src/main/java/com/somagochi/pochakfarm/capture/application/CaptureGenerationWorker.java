package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerClient;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerRequest;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerResult;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class CaptureGenerationWorker {

  private final CaptureCharacterizerClient captureCharacterizerClient;
  private final ImageUploadService imageUploadService;
  private final CaptureRepository captureRepository;
  private final TransactionTemplate transactionTemplate;

  public CaptureGenerationWorker(
      CaptureCharacterizerClient captureCharacterizerClient,
      ImageUploadService imageUploadService,
      CaptureRepository captureRepository,
      TransactionTemplate transactionTemplate) {
    this.captureCharacterizerClient = captureCharacterizerClient;
    this.imageUploadService = imageUploadService;
    this.captureRepository = captureRepository;
    this.transactionTemplate = transactionTemplate;
  }

  public void generate(CaptureGenerationCommand command) {
    try {
      PresignResponse originalDownload =
          imageUploadService.createDownloadPresign(command.userId(), command.originalImageKey());
      PresignResponse animalUpload =
          imageUploadService.createPublicPresign("capture-animal", "image/png");
      PresignResponse cardUpload =
          imageUploadService.createPublicPresign("capture-card", "image/png");

      CaptureCharacterizerResult result =
          captureCharacterizerClient.characterize(
              new CaptureCharacterizerRequest(
                  originalDownload.uploadUrl(),
                  animalUpload.uploadUrl(),
                  cardUpload.uploadUrl(),
                  command.animalName(),
                  command.cardType(),
                  command.tier(),
                  command.skill1(),
                  command.skill2(),
                  command.cardNo()));

      validateResult(animalUpload.key(), cardUpload.key(), result);
      succeed(command.captureId(), animalUpload.key(), cardUpload.key(), result.elapsedMs());
    } catch (BusinessException exception) {
      fail(command.captureId(), exception.getCode());
      log.warn(
          "capture_generation_failed captureId={} errorCode={}",
          command.captureId(),
          exception.getCode(),
          exception);
    } catch (RuntimeException exception) {
      fail(command.captureId(), ErrorCode.CHARACTERIZATION_FAILED.getCode());
      log.warn(
          "capture_generation_failed captureId={} errorCode={} exception={}",
          command.captureId(),
          ErrorCode.CHARACTERIZATION_FAILED.getCode(),
          exception.getClass().getSimpleName(),
          exception);
    }
  }

  private void validateResult(
      String animalImageKey, String cardImageKey, CaptureCharacterizerResult result) {
    if (result == null || !"success".equals(result.status())) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
    imageUploadService.validatePublicObject(animalImageKey, result.animalContentType());
    imageUploadService.validatePublicObject(cardImageKey, result.cardContentType());
  }

  private void succeed(
      Long captureId, String animalImageKey, String cardImageKey, Integer elapsedMs) {
    transactionTemplate.executeWithoutResult(
        status -> {
          Capture capture = find(captureId);
          capture.succeed(animalImageKey, cardImageKey, elapsedMs);
        });
  }

  private void fail(Long captureId, String failureReason) {
    transactionTemplate.executeWithoutResult(
        status -> {
          Capture capture = find(captureId);
          capture.fail(failureReason);
        });
  }

  private Capture find(Long captureId) {
    return captureRepository
        .findById(captureId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CAPTURE_NOT_FOUND));
  }
}
