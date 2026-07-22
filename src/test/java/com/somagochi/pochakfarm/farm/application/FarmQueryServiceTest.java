package com.somagochi.pochakfarm.farm.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.somagochi.pochakfarm.animal.application.AnimalReadService;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.domain.FarmFloor;
import com.somagochi.pochakfarm.farm.domain.FarmSlot;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.dto.FarmFloorResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSlotResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSpaceResponse;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmFloorRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSlotRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class FarmQueryServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SPACE_ID = 100L;
  private static final Long FIRST_FLOOR_ID = 1000L;
  private static final Long SECOND_FLOOR_ID = 2000L;

  private final FarmSpaceRepository farmSpaceRepository = mock(FarmSpaceRepository.class);
  private final FarmFloorRepository farmFloorRepository = mock(FarmFloorRepository.class);
  private final FarmSlotRepository farmSlotRepository = mock(FarmSlotRepository.class);
  private final AnimalReadService animalReadService = mock(AnimalReadService.class);
  private final FarmQueryService service =
      new FarmQueryService(
          farmSpaceRepository, farmFloorRepository, farmSlotRepository, animalReadService);

  @Test
  void throwsWhenUserHasNoSpaceOfTheme() {
    given(farmSpaceRepository.findByUserIdAndType(USER_ID, CardType.SEA))
        .willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE));

    assertEquals(ErrorCode.FARM_SPACE_NOT_FOUND.getCode(), exception.getCode());
    verifyNoInteractions(farmFloorRepository, farmSlotRepository, animalReadService);
  }

  @Test
  void throwsWhenPageIsBeyondLastFloor() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.getFarmSpace(USER_ID, CardType.SEA, 1));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
    verifyNoInteractions(farmSpaceRepository, farmFloorRepository, farmSlotRepository);
  }

  @Test
  void queriesFloorsWithFixedPageSize() {
    givenSpace();
    givenFloors(floor(FIRST_FLOOR_ID, 1));
    given(farmSlotRepository.findAllByFloorIdInOrderBySequenceAsc(any())).willReturn(List.of());

    service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(farmFloorRepository).findAllBySpaceId(eq(SPACE_ID), captor.capture());
    assertEquals(FarmSpace.FIRST_PAGE, captor.getValue().getPageNumber());
    assertEquals(FarmSpace.FLOOR_COUNT_PER_PAGE, captor.getValue().getPageSize());
  }

  @Test
  void marksFloorsWithoutRowAsLocked() {
    givenSpace();
    givenFloors(floor(FIRST_FLOOR_ID, 1));
    given(farmSlotRepository.findAllByFloorIdInOrderBySequenceAsc(any())).willReturn(List.of());

    FarmSpaceResponse response = service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    assertEquals(CardType.SEA, response.type());
    assertEquals(FarmSpace.FIRST_PAGE, response.page());
    assertEquals(FarmSpace.FLOOR_COUNT_PER_PAGE, response.size());
    assertEquals(FarmSpace.TOTAL_PAGE_COUNT, response.totalPages());
    assertEquals(FarmSpace.FLOOR_COUNT_PER_PAGE, response.floors().size());
    assertTrue(response.floors().get(0).unlocked());
    for (FarmFloorResponse floor : response.floors().subList(1, response.floors().size())) {
      assertFalse(floor.unlocked());
      assertTrue(floor.slots().isEmpty());
    }
  }

  @Test
  void fillsSlotsWithAnimalSummaryInSequenceOrder() {
    givenSpace();
    givenFloors(floor(FIRST_FLOOR_ID, 1), floor(SECOND_FLOOR_ID, 2));
    given(farmSlotRepository.findAllByFloorIdInOrderBySequenceAsc(any()))
        .willReturn(
            List.of(
                slot(FIRST_FLOOR_ID, 1, 11L),
                slot(FIRST_FLOOR_ID, 2, 22L),
                slot(SECOND_FLOOR_ID, 1, null)));
    given(animalReadService.getAnimals(Set.of(11L, 22L)))
        .willReturn(
            Map.of(
                11L, animal(11L, "첫번째", "https://cdn.example.com/a.png"),
                22L, animal(22L, "두번째", null)));

    FarmSpaceResponse response = service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    FarmFloorResponse firstFloor = response.floors().get(0);
    assertEquals(FarmFloor.SLOT_COUNT_PER_FLOOR, firstFloor.slots().size());
    assertEquals(1, firstFloor.slots().get(0).sequence());
    assertEquals("첫번째", firstFloor.slots().get(0).animal().animalName());
    assertEquals(
        "https://cdn.example.com/a.png", firstFloor.slots().get(0).animal().cardImageUrl());
    assertEquals(2, firstFloor.slots().get(1).sequence());
    assertEquals(22L, firstFloor.slots().get(1).animal().animalId());
    assertNull(firstFloor.slots().get(1).animal().cardImageUrl());

    assertNull(firstFloor.slots().get(2).animal());
    assertNull(firstFloor.slots().get(3).animal());

    FarmFloorResponse secondFloor = response.floors().get(1);
    assertTrue(secondFloor.unlocked());
    assertEquals(FarmFloor.SLOT_COUNT_PER_FLOOR, secondFloor.slots().size());
    assertNull(secondFloor.slots().get(0).animal());
  }

  @Test
  void looksUpOnlyAnimalsPlacedOnThePage() {
    givenSpace();
    givenFloors(floor(FIRST_FLOOR_ID, 1));
    given(farmSlotRepository.findAllByFloorIdInOrderBySequenceAsc(any()))
        .willReturn(List.of(slot(FIRST_FLOOR_ID, 1, 11L), slot(FIRST_FLOOR_ID, 2, null)));
    given(animalReadService.getAnimals(Set.of(11L)))
        .willReturn(Map.of(11L, animal(11L, "첫번째", null)));

    FarmSpaceResponse response = service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    List<FarmSlotResponse> slots = response.floors().get(0).slots();
    assertEquals(11L, slots.get(0).animal().animalId());
    assertNull(slots.get(1).animal());
    verify(animalReadService).getAnimals(Set.of(11L));
  }

  @Test
  void leavesSlotEmptyWhenCharacterizationIsMissing() {
    givenSpace();
    givenFloors(floor(FIRST_FLOOR_ID, 1));
    given(farmSlotRepository.findAllByFloorIdInOrderBySequenceAsc(any()))
        .willReturn(List.of(slot(FIRST_FLOOR_ID, 1, 11L)));
    given(animalReadService.getAnimals(Set.of(11L))).willReturn(Map.of());

    FarmSpaceResponse response = service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    assertNull(response.floors().get(0).slots().get(0).animal());
  }

  private void givenSpace() {
    given(farmSpaceRepository.findByUserIdAndType(USER_ID, CardType.SEA))
        .willReturn(Optional.of(space()));
  }

  private void givenFloors(FarmFloor... floors) {
    given(farmFloorRepository.findAllBySpaceId(eq(SPACE_ID), any(Pageable.class)))
        .willReturn(List.of(floors));
  }

  private FarmSpace space() {
    FarmSpace space = FarmSpace.create(USER_ID, CardType.SEA);
    setField(space, "id", SPACE_ID);
    return space;
  }

  private FarmFloor floor(Long id, Integer sequence) {
    FarmFloor floor = FarmFloor.create(SPACE_ID, sequence);
    setField(floor, "id", id);
    return floor;
  }

  private AnimalResponse animal(Long id, String animalName, String resultImageUrl) {
    return new AnimalResponse(
        id, animalName, CharacterizationStatus.SUCCEEDED, resultImageUrl, null, CardType.SEA, null);
  }

  private FarmSlot slot(Long floorId, Integer sequence, Long animalId) {
    FarmSlot slot = FarmSlot.create(floorId, sequence);
    setField(slot, "animalId", animalId);
    return slot;
  }
}
