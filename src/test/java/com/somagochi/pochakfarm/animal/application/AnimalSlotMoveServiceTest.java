package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

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
  private static final Long SPACE_ID = 100L;
  private static final int SOURCE_FLOOR = 1;
  private static final int SOURCE_SLOT = 1;
  private static final int TARGET_FLOOR = 2;
  private static final int TARGET_SLOT = 3;

  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final CaptureQueryService captureQueryService = mock(CaptureQueryService.class);
  private final FarmQueryService farmQueryService = mock(FarmQueryService.class);
  private final AnimalSlotMoveService service =
      new AnimalSlotMoveService(animalRepository, captureQueryService, farmQueryService);

  @Test
  void movesAnimalIntoEmptyTargetSlot() {
    Animal animal = animal(SOURCE_FLOOR, SOURCE_SLOT);
    givenAnimalWithCapture(animal, capture(USER_ID, CardType.SEA));
    givenSpace(TARGET_FLOOR);
    givenOccupant(Optional.empty());

    AnimalSlotMoveResponse response =
        service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_FLOOR, TARGET_SLOT);

    verify(animal).moveTo(SPACE_ID, TARGET_FLOOR, TARGET_SLOT);
    assertEquals(ANIMAL_ID, response.animalId());
    assertEquals(TARGET_FLOOR, response.floorNum());
    assertEquals(TARGET_SLOT, response.slotNum());
  }

  @Test
  void swapsPositionsWhenTargetIsOccupied() {
    Animal animal = animal(SOURCE_FLOOR, SOURCE_SLOT);
    Animal occupant = mock(Animal.class);
    givenAnimalWithCapture(animal, capture(USER_ID, CardType.SEA));
    givenSpace(TARGET_FLOOR);
    givenOccupant(Optional.of(occupant));

    service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_FLOOR, TARGET_SLOT);

    verify(occupant).moveTo(SPACE_ID, SOURCE_FLOOR, SOURCE_SLOT);
    verify(animal).moveTo(SPACE_ID, TARGET_FLOOR, TARGET_SLOT);
  }

  @Test
  void returnsCurrentPlacementWhenTargetIsSamePosition() {
    Animal animal = animal(TARGET_FLOOR, TARGET_SLOT);
    givenAnimalWithCapture(animal, capture(USER_ID, CardType.SEA));
    givenSpace(TARGET_FLOOR);

    AnimalSlotMoveResponse response =
        service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_FLOOR, TARGET_SLOT);

    assertEquals(TARGET_FLOOR, response.floorNum());
    assertEquals(TARGET_SLOT, response.slotNum());
    verify(animal, never()).moveTo(anyLong(), anyInt(), anyInt());
  }

  @Test
  void rejectsWhenTargetFloorIsLocked() {
    Animal animal = animal(SOURCE_FLOOR, SOURCE_SLOT);
    givenAnimalWithCapture(animal, capture(USER_ID, CardType.SEA));
    givenSpace(FarmSpace.FIRST_FLOOR);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_FLOOR, TARGET_SLOT));

    assertEquals(ErrorCode.FARM_SLOT_NOT_FOUND.getCode(), exception.getCode());
    verify(animal, never()).moveTo(anyLong(), anyInt(), anyInt());
  }

  @Test
  void rejectsWhenTargetSlotIsOutOfRange() {
    Animal animal = animal(SOURCE_FLOOR, SOURCE_SLOT);
    givenAnimalWithCapture(animal, capture(USER_ID, CardType.SEA));
    givenSpace(TARGET_FLOOR);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.moveToSlot(
                    USER_ID, ANIMAL_ID, TARGET_FLOOR, FarmSpace.SLOT_COUNT_PER_FLOOR + 1));

    assertEquals(ErrorCode.FARM_SLOT_NOT_FOUND.getCode(), exception.getCode());
    verify(animal, never()).moveTo(anyLong(), anyInt(), anyInt());
  }

  @Test
  void rejectsWhenTargetCoordinateIsMissing() {
    Animal animal = animal(SOURCE_FLOOR, SOURCE_SLOT);
    givenAnimalWithCapture(animal, capture(USER_ID, CardType.SEA));
    givenSpace(TARGET_FLOOR);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.moveToSlot(USER_ID, ANIMAL_ID, null, null));

    assertEquals(ErrorCode.FARM_SLOT_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void rejectsWhenAnimalIsNotOwnedByUser() {
    Animal animal = animal(SOURCE_FLOOR, SOURCE_SLOT);
    givenAnimalWithCapture(animal, capture(OTHER_USER_ID, CardType.SEA));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_FLOOR, TARGET_SLOT));

    assertEquals(ErrorCode.FORBIDDEN_ANIMAL_ACCESS.getCode(), exception.getCode());
  }

  @Test
  void rejectsWhenAnimalNotFound() {
    given(animalRepository.findById(ANIMAL_ID)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_FLOOR, TARGET_SLOT));

    assertEquals(ErrorCode.ANIMAL_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void propagatesWhenSpaceOfTypeIsMissing() {
    Animal animal = animal(SOURCE_FLOOR, SOURCE_SLOT);
    givenAnimalWithCapture(animal, capture(USER_ID, CardType.SEA));
    given(farmQueryService.getSpace(USER_ID, CardType.SEA))
        .willThrow(new BusinessException(ErrorCode.FARM_SPACE_NOT_FOUND));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.moveToSlot(USER_ID, ANIMAL_ID, TARGET_FLOOR, TARGET_SLOT));

    assertEquals(ErrorCode.FARM_SPACE_NOT_FOUND.getCode(), exception.getCode());
  }

  private void givenAnimalWithCapture(Animal animal, Capture capture) {
    given(animalRepository.findById(ANIMAL_ID)).willReturn(Optional.of(animal));
    given(captureQueryService.findById(CAPTURE_ID)).willReturn(Optional.of(capture));
  }

  private void givenSpace(int unlockedFloor) {
    FarmSpace space = FarmSpace.create(USER_ID, CardType.SEA);
    setField(space, "id", SPACE_ID);
    setField(space, "floor", unlockedFloor);
    given(farmQueryService.getSpace(USER_ID, CardType.SEA)).willReturn(space);
  }

  private void givenOccupant(Optional<Animal> occupant) {
    given(animalRepository.findBySpaceIdAndFloorNumAndSlotNum(SPACE_ID, TARGET_FLOOR, TARGET_SLOT))
        .willReturn(occupant);
  }

  private Animal animal(Integer floorNum, Integer slotNum) {
    Animal animal = mock(Animal.class);
    given(animal.getId()).willReturn(ANIMAL_ID);
    given(animal.getCaptureId()).willReturn(CAPTURE_ID);
    given(animal.getSpaceId()).willReturn(SPACE_ID);
    given(animal.getFloorNum()).willReturn(floorNum);
    given(animal.getSlotNum()).willReturn(slotNum);
    given(animal.isAt(any(), any(), any()))
        .willAnswer(
            invocation ->
                SPACE_ID.equals(invocation.getArgument(0))
                    && floorNum.equals(invocation.getArgument(1))
                    && slotNum.equals(invocation.getArgument(2)));
    return animal;
  }

  private Capture capture(Long userId, CardType cardType) {
    Capture capture = mock(Capture.class);
    given(capture.getUserId()).willReturn(userId);
    given(capture.getCardType()).willReturn(cardType);
    return capture;
  }
}
