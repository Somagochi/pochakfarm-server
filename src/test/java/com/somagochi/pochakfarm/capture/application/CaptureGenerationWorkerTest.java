package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerClient;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerRequest;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerResult;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class CaptureGenerationWorkerTest {

  private static final String CAPTURE_ID_VALUE = "123";

  @Mock private CaptureCharacterizerClient captureCharacterizerClient;
  @Mock private ImageUploadService imageUploadService;
  @Mock private CaptureRepository captureRepository;
  @Mock private TransactionTemplate transactionTemplate;
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Test
  void succeedsOnlyAfterBothResultObjectsAreValidated() {
    Capture capture = capture();
    CaptureGenerationWorker worker = worker(capture);
    stubPresigns();
    when(captureCharacterizerClient.characterize(any()))
        .thenReturn(
            new CaptureCharacterizerResult("success", "openai", "image/png", "image/png", 100));

    worker.generate(command(), TimeUnit.MILLISECONDS.toNanos(7));

    ArgumentCaptor<CaptureCharacterizerRequest> captor =
        ArgumentCaptor.forClass(CaptureCharacterizerRequest.class);
    verify(captureCharacterizerClient).characterize(captor.capture());
    assertEquals(CAPTURE_ID_VALUE, captor.getValue().captureId());
    assertEquals("https://upload.test/animal", captor.getValue().animalImageUploadUrl());
    assertEquals("https://upload.test/card", captor.getValue().cardImageUploadUrl());
    verify(imageUploadService)
        .validatePublicObject("public/capture-animal/animal.png", "image/png");
    verify(imageUploadService).validatePublicObject("public/capture-card/card.png", "image/png");
    assertEquals(GenerationStatus.SUCCEEDED, capture.getGenerationStatus());
    assertEquals("public/capture-animal/animal.png", capture.getAnimalImage());
    assertEquals("public/capture-card/card.png", capture.getCardImage());
    assertEquals(100, capture.getElapsedMs());
    assertTimerCount("queue", "success", 1L);
    assertEquals(
        7.0,
        meterRegistry
            .get(CaptureGenerationMetrics.METRIC_NAME)
            .tag("stage", "queue")
            .tag("outcome", "success")
            .timer()
            .totalTime(TimeUnit.MILLISECONDS));
    assertTimerCount("characterizer", "success", 1L);
    assertTimerCount("total", "success", 1L);
    assertNull(MDC.get(CaptureGenerationWorker.CAPTURE_ID_MDC_KEY));
  }

  @Test
  void storesFailureCodeWithoutSuccessWhenResultValidationFails() {
    Capture capture = capture();
    CaptureGenerationWorker worker = worker(capture);
    stubPresigns();
    when(captureCharacterizerClient.characterize(any()))
        .thenReturn(
            new CaptureCharacterizerResult("success", "openai", "image/png", "image/png", 100));
    org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FILE_NOT_FOUND))
        .when(imageUploadService)
        .validatePublicObject("public/capture-animal/animal.png", "image/png");

    worker.generate(command(), TimeUnit.MILLISECONDS.toNanos(7));

    assertEquals(GenerationStatus.FAILED, capture.getGenerationStatus());
    assertEquals(ErrorCode.FILE_NOT_FOUND.getCode(), capture.getFailureReason());
    assertTimerCount("queue", "failure", 1L);
    assertTimerCount("characterizer", "success", 1L);
    assertTimerCount("total", "failure", 1L);
    assertNull(MDC.get(CaptureGenerationWorker.CAPTURE_ID_MDC_KEY));
  }

  @Test
  void recordsCharacterizerFailureAndClearsCaptureId() {
    Capture capture = capture();
    CaptureGenerationWorker worker = worker(capture);
    stubPresigns();
    when(captureCharacterizerClient.characterize(any()))
        .thenThrow(new BusinessException(ErrorCode.CHARACTERIZATION_FAILED));

    worker.generate(command(), TimeUnit.MILLISECONDS.toNanos(7));

    assertEquals(GenerationStatus.FAILED, capture.getGenerationStatus());
    assertEquals(ErrorCode.CHARACTERIZATION_FAILED.getCode(), capture.getFailureReason());
    assertTimerCount("queue", "failure", 1L);
    assertTimerCount("characterizer", "failure", 1L);
    assertTimerCount("total", "failure", 1L);
    assertNull(MDC.get(CaptureGenerationWorker.CAPTURE_ID_MDC_KEY));
  }

  private CaptureGenerationWorker worker(Capture capture) {
    when(captureRepository.findById(123L)).thenReturn(Optional.of(capture));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              Consumer<TransactionStatus> callback = invocation.getArgument(0);
              callback.accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());
    return new CaptureGenerationWorker(
        captureCharacterizerClient,
        imageUploadService,
        captureRepository,
        transactionTemplate,
        new CaptureGenerationMetrics(meterRegistry));
  }

  private void stubPresigns() {
    when(imageUploadService.createDownloadPresign(1L, "images/capture-original/1/original.jpg"))
        .thenReturn(
            new PresignResponse(
                "https://download.test/original",
                "images/capture-original/1/original.jpg",
                Instant.EPOCH));
    when(imageUploadService.createPublicPresign("capture-animal", "image/png"))
        .thenReturn(
            new PresignResponse(
                "https://upload.test/animal", "public/capture-animal/animal.png", Instant.EPOCH));
    when(imageUploadService.createPublicPresign("capture-card", "image/png"))
        .thenReturn(
            new PresignResponse(
                "https://upload.test/card", "public/capture-card/card.png", Instant.EPOCH));
  }

  private CaptureGenerationCommand command() {
    return new CaptureGenerationCommand(
        123L,
        1L,
        "images/capture-original/1/original.jpg",
        "두부",
        CardType.GROUND,
        Tier.B,
        CardSkill.GROUND_PAW_STRIKE,
        CardSkill.GROUND_LEAF_GUARD,
        "123",
        System.nanoTime());
  }

  private void assertTimerCount(String stage, String outcome, long expected) {
    assertEquals(
        expected,
        meterRegistry
            .get(CaptureGenerationMetrics.METRIC_NAME)
            .tag("stage", stage)
            .tag("outcome", outcome)
            .timer()
            .count());
  }

  private Capture capture() {
    Capture capture =
        Capture.create(
            1L,
            UUID.randomUUID().toString(),
            CardType.GROUND,
            Tier.B,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "123",
            "images/capture-original/1/original.jpg",
            "image/jpeg",
            Instant.parse("2026-07-24T01:05:00Z"));
    ReflectionTestUtils.setField(capture, "id", 123L);
    capture.markProcessing();
    return capture;
  }
}
