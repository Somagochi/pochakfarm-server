package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.config.AsyncConfig;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
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
      submitAfterCommit(command);
    } catch (TaskRejectedException exception) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_BUSY);
    }
    return CaptureCompleteResponse.from(capture);
  }

  private void submitAfterCommit(CaptureGenerationCommand command) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      taskExecutor.execute(() -> captureGenerationWorker.generate(command));
      return;
    }

    CountDownLatch transactionCompleted = new CountDownLatch(1);
    AtomicBoolean committed = new AtomicBoolean(false);
    taskExecutor.execute(
        () -> {
          awaitTransactionCompletion(command.captureId(), transactionCompleted);
          if (committed.get()) {
            captureGenerationWorker.generate(command);
          }
        });
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            committed.set(status == STATUS_COMMITTED);
            transactionCompleted.countDown();
          }
        });
  }

  private void awaitTransactionCompletion(Long captureId, CountDownLatch transactionCompleted) {
    try {
      transactionCompleted.await();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      log.warn("capture_generation_submission_interrupted captureId={}", captureId, exception);
    }
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
