package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class CaptureCompleteServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long CAPTURE_ID = 123L;
  private static final String ORIGINAL_IMAGE_KEY = "images/capture-original/1/original.jpg";

  @Mock private CaptureRepository captureRepository;
  @Mock private ImageUploadService imageUploadService;
  @Mock private CaptureGenerationWorker captureGenerationWorker;

  @Test
  void marksProcessingAndSubmitsGenerationWhenWaitingUploadObjectIsValid() {
    Capture capture = capture();
    ThreadPoolTaskExecutor executor = directExecutor();
    CaptureCompleteService service = service(executor);
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));

    CaptureCompleteResponse response = service.completeOriginalImage(USER_ID, CAPTURE_ID);

    assertEquals(CAPTURE_ID, response.captureId());
    assertEquals(GenerationStatus.PROCESSING, response.generationStatus());
    assertEquals(GenerationStatus.PROCESSING, capture.getGenerationStatus());
    verify(imageUploadService).validateUploadedObject(USER_ID, ORIGINAL_IMAGE_KEY, "image/jpeg");
    verify(captureGenerationWorker, timeout(1_000)).generate(any(CaptureGenerationCommand.class));
    executor.shutdown();
  }

  @Test
  void returnsCurrentStatusWithoutSubmittingWhenAlreadyProcessing() {
    Capture capture = capture();
    capture.markProcessing();
    ThreadPoolTaskExecutor executor = directExecutor();
    CaptureCompleteService service = service(executor);
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));

    CaptureCompleteResponse response = service.completeOriginalImage(USER_ID, CAPTURE_ID);

    assertEquals(GenerationStatus.PROCESSING, response.generationStatus());
    verify(imageUploadService, never()).validateUploadedObject(any(), any(), any());
    verify(captureGenerationWorker, never()).generate(any());
    executor.shutdown();
  }

  @Test
  void keepsWaitingUploadWhenOriginalValidationFails() {
    Capture capture = capture();
    ThreadPoolTaskExecutor executor = directExecutor();
    CaptureCompleteService service = service(executor);
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FILE_NOT_FOUND))
        .when(imageUploadService)
        .validateUploadedObject(USER_ID, ORIGINAL_IMAGE_KEY, "image/jpeg");

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.completeOriginalImage(USER_ID, CAPTURE_ID));

    assertEquals(ErrorCode.FILE_NOT_FOUND.getCode(), exception.getCode());
    assertEquals(GenerationStatus.WAITING_UPLOAD, capture.getGenerationStatus());
    verify(captureGenerationWorker, never()).generate(any());
    executor.shutdown();
  }

  @Test
  void convertsExecutorRejectionToBusyError() {
    Capture capture = capture();
    ThreadPoolTaskExecutor executor = org.mockito.Mockito.mock(ThreadPoolTaskExecutor.class);
    CaptureCompleteService service = service(executor);
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));
    org.mockito.Mockito.doThrow(new TaskRejectedException("full"))
        .when(executor)
        .execute(any(Runnable.class));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.completeOriginalImage(USER_ID, CAPTURE_ID));

    assertEquals(ErrorCode.CHARACTERIZATION_BUSY.getCode(), exception.getCode());
    verify(captureGenerationWorker, never()).generate(any());
  }

  @Test
  void submitsGenerationButRunsItOnlyAfterTransactionCommit() {
    Capture capture = capture();
    ThreadPoolTaskExecutor executor = directExecutor();
    CaptureCompleteService service = service(executor);
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));

    TransactionSynchronizationManager.initSynchronization();
    try {
      service.completeOriginalImage(USER_ID, CAPTURE_ID);

      verify(captureGenerationWorker, never()).generate(any());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(
              synchronization ->
                  synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
      verify(captureGenerationWorker, timeout(1_000)).generate(any(CaptureGenerationCommand.class));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
      executor.shutdown();
    }
  }

  @Test
  void doesNotRunGenerationWhenTransactionRollsBackAfterSubmission() {
    Capture capture = capture();
    ThreadPoolTaskExecutor executor = directExecutor();
    CaptureCompleteService service = service(executor);
    when(captureRepository.findByIdForUpdate(CAPTURE_ID)).thenReturn(Optional.of(capture));

    TransactionSynchronizationManager.initSynchronization();
    try {
      service.completeOriginalImage(USER_ID, CAPTURE_ID);

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(
              synchronization ->
                  synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verify(captureGenerationWorker, never()).generate(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
      executor.shutdown();
    }
  }

  private CaptureCompleteService service(ThreadPoolTaskExecutor executor) {
    return new CaptureCompleteService(
        captureRepository, imageUploadService, captureGenerationWorker, executor);
  }

  private ThreadPoolTaskExecutor directExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(0);
    executor.initialize();
    return executor;
  }

  private Capture capture() {
    Capture capture =
        Capture.create(
            USER_ID,
            UUID.randomUUID().toString(),
            CardType.GROUND,
            Tier.B,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "123",
            ORIGINAL_IMAGE_KEY,
            "image/jpeg",
            Instant.parse("2026-07-24T01:05:00Z"));
    ReflectionTestUtils.setField(capture, "id", CAPTURE_ID);
    return capture;
  }
}
