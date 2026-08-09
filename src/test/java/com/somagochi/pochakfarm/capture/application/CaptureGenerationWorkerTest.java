package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class CaptureGenerationWorkerTest {

  @Mock private CaptureCharacterizerClient captureCharacterizerClient;
  @Mock private ImageUploadService imageUploadService;
  @Mock private CaptureRepository captureRepository;
  @Mock private TransactionTemplate transactionTemplate;

  @Test
  void succeedsOnlyAfterBothResultObjectsAreValidated() {
    Capture capture = capture();
    CaptureGenerationWorker worker = worker(capture);
    stubPresigns();
    when(captureCharacterizerClient.characterize(any()))
        .thenReturn(
            new CaptureCharacterizerResult("success", "openai", "image/png", "image/png", 100));

    worker.generate(command());

    ArgumentCaptor<CaptureCharacterizerRequest> captor =
        ArgumentCaptor.forClass(CaptureCharacterizerRequest.class);
    verify(captureCharacterizerClient).characterize(captor.capture());
    assertEquals("https://upload.test/animal", captor.getValue().animalImageUploadUrl());
    assertEquals("https://upload.test/card", captor.getValue().cardImageUploadUrl());
    verify(imageUploadService)
        .validatePublicObject("public/capture-animal/animal.png", "image/png");
    verify(imageUploadService).validatePublicObject("public/capture-card/card.png", "image/png");
    assertEquals(GenerationStatus.SUCCEEDED, capture.getGenerationStatus());
    assertEquals("public/capture-animal/animal.png", capture.getAnimalImage());
    assertEquals("public/capture-card/card.png", capture.getCardImage());
    assertEquals(100, capture.getElapsedMs());
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

    worker.generate(command());

    assertEquals(GenerationStatus.FAILED, capture.getGenerationStatus());
    assertEquals(ErrorCode.FILE_NOT_FOUND.getCode(), capture.getFailureReason());
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
        captureCharacterizerClient, imageUploadService, captureRepository, transactionTemplate);
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
        "123");
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
