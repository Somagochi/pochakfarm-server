package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CaptureProperties;
import com.somagochi.pochakfarm.common.properties.CaptureProperties.Reaper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class CaptureReaperTest {

  private static final Duration STALE_THRESHOLD = Duration.ofMinutes(30);

  private final CaptureRepository captureRepository = mock(CaptureRepository.class);
  private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
  private final CaptureReaper reaper =
      new CaptureReaper(
          captureRepository,
          transactionTemplate,
          new CaptureProperties(new Reaper(true, Duration.ofMinutes(1), STALE_THRESHOLD)));

  @BeforeEach
  void setUp() {
    doAnswer(
            invocation -> {
              Consumer<TransactionStatus> callback = invocation.getArgument(0);
              callback.accept(mock(TransactionStatus.class));
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());
  }

  @Test
  void findsCapturesProcessingSinceBeforeThreshold() {
    given(captureRepository.findStaleProcessingIds(any(), any())).willReturn(List.of());

    Instant before = Instant.now();
    reaper.reapStaleProcessing();
    Instant after = Instant.now();

    ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(captureRepository)
        .findStaleProcessingIds(eq(GenerationStatus.PROCESSING), thresholdCaptor.capture());
    Instant threshold = thresholdCaptor.getValue();
    assertTrue(
        !threshold.isBefore(before.minus(STALE_THRESHOLD))
            && !threshold.isAfter(after.minus(STALE_THRESHOLD)),
        "threshold는 실행 시각에서 stale-threshold만큼 이전이어야 한다");
    verify(captureRepository, never()).findByIdForUpdate(any());
  }

  @Test
  void failsStaleProcessingCaptureAsTimedOut() {
    Capture capture = processingCapture(Instant.now().minus(Duration.ofMinutes(31)));
    given(captureRepository.findStaleProcessingIds(any(), any())).willReturn(List.of(1L));
    given(captureRepository.findByIdForUpdate(1L)).willReturn(Optional.of(capture));

    reaper.reapStaleProcessing();

    assertEquals(GenerationStatus.FAILED, capture.getGenerationStatus());
    assertEquals(ErrorCode.CHARACTERIZATION_TIMED_OUT.getCode(), capture.getFailureReason());
    assertNull(capture.getCardImage());
  }

  @Test
  void doesNotOverwriteCaptureThatSucceededAfterLookup() {
    Capture capture = processingCapture(Instant.now().minus(Duration.ofMinutes(31)));
    capture.succeed("public/capture-animal/a.png", "public/capture-card/c.png", 100);
    given(captureRepository.findStaleProcessingIds(any(), any())).willReturn(List.of(1L));
    given(captureRepository.findByIdForUpdate(1L)).willReturn(Optional.of(capture));

    reaper.reapStaleProcessing();

    assertEquals(GenerationStatus.SUCCEEDED, capture.getGenerationStatus());
    assertEquals("public/capture-card/c.png", capture.getCardImage());
    assertNull(capture.getFailureReason());
  }

  @Test
  void doesNotFailCaptureWhoseProcessingRestartedAfterThreshold() {
    Capture capture = processingCapture(Instant.now());
    given(captureRepository.findStaleProcessingIds(any(), any())).willReturn(List.of(1L));
    given(captureRepository.findByIdForUpdate(1L)).willReturn(Optional.of(capture));

    reaper.reapStaleProcessing();

    assertEquals(GenerationStatus.PROCESSING, capture.getGenerationStatus());
  }

  @Test
  void skipsCaptureDeletedAfterLookup() {
    given(captureRepository.findStaleProcessingIds(any(), any())).willReturn(List.of(1L));
    given(captureRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    reaper.reapStaleProcessing();

    verify(captureRepository).findByIdForUpdate(1L);
  }

  private Capture processingCapture(Instant processingStartedAt) {
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
    capture.markProcessing(processingStartedAt);
    return capture;
  }
}
