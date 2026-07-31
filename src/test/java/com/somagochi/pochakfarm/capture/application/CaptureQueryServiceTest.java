package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CaptureQueryServiceTest {

  private final CaptureRepository captureRepository = mock(CaptureRepository.class);
  private final FileStorage fileStorage = mock(FileStorage.class);
  private final CaptureQueryService service =
      new CaptureQueryService(captureRepository, fileStorage);

  @Test
  void returnsNullImageUrlsBeforeGenerationSucceeds() {
    Capture capture = capture();
    when(captureRepository.findById(123L)).thenReturn(Optional.of(capture));

    CaptureResponse response = service.getCapture(1L, 123L);

    assertNull(response.sceneImageUrl());
    assertNull(response.cardImageUrl());
  }

  @Test
  void returnsImageUrlsWhenGenerationSucceeds() {
    Capture capture = capture();
    capture.succeed("public/capture-scene/scene.png", "public/capture-card/card.png", 100);
    when(captureRepository.findById(123L)).thenReturn(Optional.of(capture));
    when(fileStorage.buildUrl("public/capture-scene/scene.png"))
        .thenReturn("https://cdn.test/scene.png");
    when(fileStorage.buildUrl("public/capture-card/card.png"))
        .thenReturn("https://cdn.test/card.png");

    CaptureResponse response = service.getCapture(1L, 123L);

    assertEquals("https://cdn.test/scene.png", response.sceneImageUrl());
    assertEquals("https://cdn.test/card.png", response.cardImageUrl());
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
    return capture;
  }
}
