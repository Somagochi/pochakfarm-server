package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.config.AsyncConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.transaction.AfterCommitExecutor;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaptureCompleteService {

  private final CaptureRepository captureRepository;
  private final ImageUploadService imageUploadService;
  private final CaptureGenerationWorker captureGenerationWorker;
  private final ThreadPoolTaskExecutor taskExecutor;

  public CaptureCompleteService(
      CaptureRepository captureRepository,
      ImageUploadService imageUploadService,
      CaptureGenerationWorker captureGenerationWorker,
      @Qualifier(AsyncConfig.CHARACTERIZATION_EXECUTOR) ThreadPoolTaskExecutor taskExecutor) {
    this.captureRepository = captureRepository;
    this.imageUploadService = imageUploadService;
    this.captureGenerationWorker = captureGenerationWorker;
    this.taskExecutor = taskExecutor;
  }

  @Transactional
  public CaptureCompleteResponse completeOriginalImage(Long userId, Long captureId) {
    Capture capture =
        captureRepository
            .findByIdForUpdate(captureId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CAPTURE_NOT_FOUND));
    validateOwnership(capture, userId);
    if (!capture.isWaitingUpload()) {
      return CaptureCompleteResponse.from(capture);
    }

    imageUploadService.validateUploadedObject(
        userId, capture.getOriginalImageKey(), capture.getOriginalImageContentType());
    capture.markProcessing();
    CaptureGenerationCommand command = commandFrom(capture);
    try {
      AfterCommitExecutor.executeAsyncAfterCommit(
          taskExecutor, () -> captureGenerationWorker.generate(command));
    } catch (TaskRejectedException exception) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_BUSY);
    }
    return CaptureCompleteResponse.from(capture);
  }

  private CaptureGenerationCommand commandFrom(Capture capture) {
    return new CaptureGenerationCommand(
        capture.getId(),
        capture.getUserId(),
        capture.getOriginalImageKey(),
        capture.getAnimalName(),
        capture.getCardType(),
        capture.getTier(),
        capture.getSkill1(),
        capture.getSkill2(),
        capture.getCardNo());
  }

  private void validateOwnership(Capture capture, Long userId) {
    if (!capture.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_CAPTURE_ACCESS);
    }
  }
}
