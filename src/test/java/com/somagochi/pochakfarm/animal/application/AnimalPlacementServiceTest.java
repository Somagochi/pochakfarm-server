package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnimalPlacementServiceTest {

  private static final long USER_ID = 1L;
  private static final long SPACE_ID = 20L;
  private static final long CAPTURE_ID = 30L;
  private static final long REPLACED_ANIMAL_ID = 40L;

  @Mock private AnimalRepository animalRepository;
  @Mock private CaptureRepository captureRepository;
  @Mock private FarmQueryService farmQueryService;

  private AnimalPlacementService service;

  @BeforeEach
  void setUp() {
    service = new AnimalPlacementService(animalRepository, captureRepository, farmQueryService);
  }

  @Test
  void placesAnimalAtEmptyUnlockedSlot() {
    givenSpace();
    given(animalRepository.findBySpaceIdAndFloorNumAndSlotNumForUpdate(SPACE_ID, 1, 2))
        .willReturn(Optional.empty());
    given(animalRepository.save(any(Animal.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    Animal animal = service.placeAt(USER_ID, CardType.GROUND, CAPTURE_ID, 1, 2);

    assertEquals(CAPTURE_ID, animal.getCaptureId());
    assertEquals(SPACE_ID, animal.getSpaceId());
    assertEquals(1, animal.getFloorNum());
    assertEquals(2, animal.getSlotNum());
  }

  @Test
  void rejectsOccupiedSlotWithoutExplicitReplacement() {
    givenSpace();
    given(animalRepository.findBySpaceIdAndFloorNumAndSlotNumForUpdate(SPACE_ID, 1, 2))
        .willReturn(Optional.of(animal(REPLACED_ANIMAL_ID, 99L)));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.placeAt(USER_ID, CardType.GROUND, CAPTURE_ID, 1, 2));

    assertEquals(ErrorCode.FARM_SLOT_OCCUPIED.getCode(), exception.getCode());
  }

  @Test
  void hardDeletesExplicitOccupantAndPlacesNewAnimal() {
    givenSpace();
    Animal occupant = animal(REPLACED_ANIMAL_ID, 99L);
    Capture replacedCapture = org.mockito.Mockito.mock(Capture.class);
    given(animalRepository.findBySpaceIdAndFloorNumAndSlotNumForUpdate(SPACE_ID, 1, 2))
        .willReturn(Optional.of(occupant));
    given(captureRepository.findById(99L)).willReturn(Optional.of(replacedCapture));
    given(replacedCapture.isOwnedBy(USER_ID)).willReturn(true);
    given(replacedCapture.getCardType()).willReturn(CardType.GROUND);
    given(animalRepository.save(any(Animal.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    Animal animal =
        service.replaceAt(USER_ID, CardType.GROUND, CAPTURE_ID, REPLACED_ANIMAL_ID, 1, 2);

    verify(animalRepository).delete(occupant);
    verify(animalRepository).flush();
    assertEquals(CAPTURE_ID, animal.getCaptureId());
  }

  @Test
  void rejectsWhenCurrentOccupantDiffersFromRequestedAnimal() {
    givenSpace();
    Animal occupant = animal(999L, 99L);
    given(animalRepository.findBySpaceIdAndFloorNumAndSlotNumForUpdate(SPACE_ID, 1, 2))
        .willReturn(Optional.of(occupant));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.replaceAt(USER_ID, CardType.GROUND, CAPTURE_ID, REPLACED_ANIMAL_ID, 1, 2));

    assertEquals(ErrorCode.ANIMAL_REPLACEMENT_CONFLICT.getCode(), exception.getCode());
    verify(animalRepository, never()).delete(any());
  }

  @Test
  void rejectsLockedFloor() {
    givenSpace();

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.placeAt(USER_ID, CardType.GROUND, CAPTURE_ID, 2, 1));

    assertEquals(ErrorCode.FARM_SLOT_NOT_FOUND.getCode(), exception.getCode());
  }

  private void givenSpace() {
    FarmSpace space = FarmSpace.create(USER_ID, CardType.GROUND);
    ReflectionTestUtils.setField(space, "id", SPACE_ID);
    given(farmQueryService.getSpaceForUpdate(USER_ID, CardType.GROUND)).willReturn(space);
  }

  private Animal animal(long animalId, long captureId) {
    Animal animal = Animal.create(captureId, SPACE_ID, 1, 2);
    ReflectionTestUtils.setField(animal, "id", animalId);
    return animal;
  }
}
