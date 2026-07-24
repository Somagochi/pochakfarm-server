package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.application.CaptureQueryService;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnimalSlotMoveServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long ANIMAL_ID = 10L;
  private static final Long CAPTURE_ID = 20L;
  private static final Long SOURCE_SLOT_ID = 30L;
  private static final Long TARGET_SLOT_ID = 40L;

  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final CaptureQueryService captureQueryService = mock(CaptureQueryService.class);
  private final FarmQueryService farmQueryService = mock(FarmQueryService.class);
  private final AnimalSlotMoveService service =
      new AnimalSlotMoveService(animalRepository, captureQueryService, farmQueryService);

  @Test
  void movesAnimalIntoEmptyTargetSlot() {
    Animal animal = animal(SOURCE_SLOT_ID);
    Capture capture = capture(USER_ID, CardType.SEA);
    givenAnimalWithCapture(animal, capture);
    givenTargetSpace(CardType.SEA);
    given(animalRepository.findBySlotId(TARGET_SLOT_ID)).willReturn(Optional.empty());

    AnimalSlotMoveResponse response = service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_SLOT_ID);

    verify(animal).moveTo(TARGET_SLOT_ID);
    assertEquals(ANIMAL_ID, response.animalId());
    assertEquals(TARGET_SLOT_ID, response.slotId());
  }

  @Test
  void swapsSlotsWhenTargetIsOccupied() {
    Animal animal = animal(SOURCE_SLOT_ID);
    Capture capture = capture(USER_ID, CardType.SEA);
    Animal occupant = mock(Animal.class);
    givenAnimalWithCapture(animal, capture);
    givenTargetSpace(CardType.SEA);
    given(animalRepository.findBySlotId(TARGET_SLOT_ID)).willReturn(Optional.of(occupant));

    service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_SLOT_ID);

    verify(occupant).moveTo(SOURCE_SLOT_ID);
    verify(animal).moveTo(TARGET_SLOT_ID);
  }

  @Test
  void returnsCurrentPlacementWhenTargetIsSameSlot() {
    Animal animal = animal(TARGET_SLOT_ID);
    Capture capture = capture(USER_ID, CardType.SEA);
    givenAnimalWithCapture(animal, capture);

    AnimalSlotMoveResponse response = service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_SLOT_ID);

    assertEquals(TARGET_SLOT_ID, response.slotId());
    verify(animal, never()).moveTo(TARGET_SLOT_ID);
  }

  @Test
  void rejectsWhenTypeDiffersFromTargetSpace() {
    Animal animal = animal(SOURCE_SLOT_ID);
    Capture capture = capture(USER_ID, CardType.SEA);
    givenAnimalWithCapture(animal, capture);
    givenTargetSpace(CardType.SKY);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_SLOT_ID));

    assertEquals(ErrorCode.FARM_SLOT_TYPE_MISMATCH.getCode(), exception.getCode());
    verify(animal, never()).moveTo(TARGET_SLOT_ID);
  }

  @Test
  void rejectsWhenAnimalIsNotOwnedByUser() {
    Animal animal = animal(SOURCE_SLOT_ID);
    Capture capture = capture(OTHER_USER_ID, CardType.SEA);
    givenAnimalWithCapture(animal, capture);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_SLOT_ID));

    assertEquals(ErrorCode.FORBIDDEN_ANIMAL_ACCESS.getCode(), exception.getCode());
  }

  @Test
  void rejectsWhenTargetSlotNotFound() {
    Animal animal = animal(SOURCE_SLOT_ID);
    Capture capture = capture(USER_ID, CardType.SEA);
    givenAnimalWithCapture(animal, capture);
    given(farmQueryService.findSpaceBySlotId(TARGET_SLOT_ID)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_SLOT_ID));

    assertEquals(ErrorCode.FARM_SLOT_NOT_FOUND.getCode(), exception.getCode());
  }

  private void givenAnimalWithCapture(Animal animal, Capture capture) {
    given(animalRepository.findById(ANIMAL_ID)).willReturn(Optional.of(animal));
    given(captureQueryService.findById(CAPTURE_ID)).willReturn(Optional.of(capture));
  }

  private void givenTargetSpace(CardType type) {
    FarmSpace space = mock(FarmSpace.class);
    given(space.getUserId()).willReturn(USER_ID);
    given(space.getType()).willReturn(type);
    given(farmQueryService.findSpaceBySlotId(TARGET_SLOT_ID)).willReturn(Optional.of(space));
  }

  private Animal animal(Long slotId) {
    Animal animal = mock(Animal.class);
    given(animal.getId()).willReturn(ANIMAL_ID);
    given(animal.getCaptureId()).willReturn(CAPTURE_ID);
    given(animal.getSlotId()).willReturn(slotId);
    return animal;
  }

  private Capture capture(Long userId, CardType cardType) {
    Capture capture = mock(Capture.class);
    given(capture.getUserId()).willReturn(userId);
    given(capture.getCardType()).willReturn(cardType);
    return capture;
  }
}
