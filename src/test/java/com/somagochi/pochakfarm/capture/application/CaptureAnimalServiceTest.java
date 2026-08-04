package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.animal.application.AnimalPlacementService;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CaptureAnimalServiceTest {

  private static final long USER_ID = 1L;
  private static final long CAPTURE_ID = 123L;
  private static final long ANIMAL_ID = 10L;
  private static final String ANIMAL_IMAGE_KEY = "public/capture-animal/1/123.png";

  @Mock private CaptureRepository captureRepository;
  @Mock private AnimalRepository animalRepository;
  @Mock private AnimalPlacementService animalPlacementService;
  @Mock private ImageUploadService imageUploadService;
  @Mock private FileStorage fileStorage;

  private CaptureAnimalService service;

  @BeforeEach
  void setUp() {
    service =
        new CaptureAnimalService(
            captureRepository,
            animalRepository,
            animalPlacementService,
            imageUploadService,
            fileStorage);
  }

  @Test
  void presignsFixedPngKeyForPlaceableCapture() {
    Capture capture = placeableCapture(USER_ID);
    given(captureRepository.findById(CAPTURE_ID)).willReturn(Optional.of(capture));
    PresignResponse expected =
        new PresignResponse("https://upload.test/animal", ANIMAL_IMAGE_KEY, Instant.EPOCH);
    given(imageUploadService.refreshPresign(USER_ID, ANIMAL_IMAGE_KEY, "image/png"))
        .willReturn(expected);

    PresignResponse response = service.presign(USER_ID, CAPTURE_ID);

    assertEquals(expected, response);
  }

  @Test
  void placesAnimalInEmptySelectedSlotAndRegistersImage() {
    Capture capture = placeableCapture(USER_ID);
    Animal animal = animal(ANIMAL_ID, CAPTURE_ID, 1, 2);
    given(captureRepository.findByIdForUpdate(CAPTURE_ID)).willReturn(Optional.of(capture));
    given(animalRepository.findByCaptureId(CAPTURE_ID)).willReturn(Optional.empty());
    given(animalPlacementService.placeAt(USER_ID, CardType.GROUND, CAPTURE_ID, 1, 2))
        .willReturn(animal);
    given(fileStorage.buildUrl(ANIMAL_IMAGE_KEY)).willReturn("https://cdn.test/animal.png");
    CaptureAnimalPlacementRequest request = request(1, 2, null);

    CaptureAnimalPlacementResponse response = service.place(USER_ID, CAPTURE_ID, request);

    assertEquals(ANIMAL_IMAGE_KEY, capture.getAnimalImage());
    assertEquals(ANIMAL_ID, response.animalId());
    verify(imageUploadService).validateUploadedObject(USER_ID, ANIMAL_IMAGE_KEY, "image/png");
  }

  @Test
  void replacesOnlyExplicitlySelectedAnimal() {
    Capture capture = placeableCapture(USER_ID);
    Animal animal = animal(ANIMAL_ID, CAPTURE_ID, 1, 2);
    given(captureRepository.findByIdForUpdate(CAPTURE_ID)).willReturn(Optional.of(capture));
    given(animalRepository.findByCaptureId(CAPTURE_ID)).willReturn(Optional.empty());
    given(animalPlacementService.replaceAt(USER_ID, CardType.GROUND, CAPTURE_ID, 99L, 1, 2))
        .willReturn(animal);
    CaptureAnimalPlacementRequest request = request(1, 2, 99L);

    service.place(USER_ID, CAPTURE_ID, request);

    verify(animalPlacementService).replaceAt(USER_ID, CardType.GROUND, CAPTURE_ID, 99L, 1, 2);
  }

  @Test
  void returnsExistingAnimalForIdenticalRetry() {
    Capture capture = placeableCapture(USER_ID);
    capture.registerAnimalImage(ANIMAL_IMAGE_KEY);
    Animal animal = animal(ANIMAL_ID, CAPTURE_ID, 1, 2);
    given(captureRepository.findByIdForUpdate(CAPTURE_ID)).willReturn(Optional.of(capture));
    given(animalRepository.findByCaptureId(CAPTURE_ID)).willReturn(Optional.of(animal));

    CaptureAnimalPlacementResponse response =
        service.place(USER_ID, CAPTURE_ID, request(1, 2, 99L));

    assertEquals(ANIMAL_ID, response.animalId());
    verify(imageUploadService, never())
        .validateUploadedObject(USER_ID, ANIMAL_IMAGE_KEY, "image/png");
  }

  @Test
  void rejectsRetryThatTargetsDifferentSlot() {
    Capture capture = placeableCapture(USER_ID);
    capture.registerAnimalImage(ANIMAL_IMAGE_KEY);
    given(captureRepository.findByIdForUpdate(CAPTURE_ID)).willReturn(Optional.of(capture));
    given(animalRepository.findByCaptureId(CAPTURE_ID))
        .willReturn(Optional.of(animal(ANIMAL_ID, CAPTURE_ID, 1, 2)));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.place(USER_ID, CAPTURE_ID, request(1, 3, null)));

    assertEquals(ErrorCode.CAPTURE_PLACEMENT_CONFLICT.getCode(), exception.getCode());
  }

  @Test
  void rejectsCaptureBeforeGameSucceeds() {
    Capture capture = generatedCapture(USER_ID);
    given(captureRepository.findById(CAPTURE_ID)).willReturn(Optional.of(capture));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.presign(USER_ID, CAPTURE_ID));

    assertEquals(ErrorCode.CAPTURE_NOT_PLACEABLE.getCode(), exception.getCode());
  }

  @Test
  void rejectsAnotherUsersCapture() {
    given(captureRepository.findById(CAPTURE_ID)).willReturn(Optional.of(placeableCapture(2L)));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.presign(USER_ID, CAPTURE_ID));

    assertEquals(ErrorCode.FORBIDDEN_CAPTURE_ACCESS.getCode(), exception.getCode());
  }

  private CaptureAnimalPlacementRequest request(int floorNum, int slotNum, Long replacedAnimalId) {
    return new CaptureAnimalPlacementRequest(ANIMAL_IMAGE_KEY, floorNum, slotNum, replacedAnimalId);
  }

  private Capture placeableCapture(Long userId) {
    Capture capture = generatedCapture(userId);
    capture.completeGame(GameStatus.SUCCEEDED, 10);
    return capture;
  }

  private Capture generatedCapture(Long userId) {
    Capture capture =
        Capture.create(
            userId,
            UUID.randomUUID().toString(),
            CardType.GROUND,
            Tier.B,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "123",
            "images/capture-original/1/original.jpg",
            "image/jpeg",
            Instant.parse("2026-08-03T01:05:00Z"));
    ReflectionTestUtils.setField(capture, "id", CAPTURE_ID);
    capture.succeed("public/capture-scene/scene.png", "public/capture-card/card.png", 100);
    return capture;
  }

  private Animal animal(long id, long captureId, int floorNum, int slotNum) {
    Animal animal = Animal.create(captureId, 50L, floorNum, slotNum);
    ReflectionTestUtils.setField(animal, "id", id);
    return animal;
  }
}
