package com.somagochi.pochakfarm.farm.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.dto.FarmFloorResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSlotResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSpaceResponse;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FarmQueryServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SPACE_ID = 100L;
  private static final int LAST_FLOOR_OF_FIRST_PAGE = FarmSpace.FLOOR_COUNT_PER_PAGE;

  private final FarmSpaceRepository farmSpaceRepository = mock(FarmSpaceRepository.class);
  private final AnimalQueryService animalQueryService = mock(AnimalQueryService.class);
  private final FarmQueryService service =
      new FarmQueryService(farmSpaceRepository, animalQueryService);

  @Test
  void throwsWhenUserHasNoSpaceOfTheme() {
    given(farmSpaceRepository.findByUserIdAndType(USER_ID, CardType.SEA))
        .willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE));

    assertEquals(ErrorCode.FARM_SPACE_NOT_FOUND.getCode(), exception.getCode());
    verifyNoInteractions(animalQueryService);
  }

  @Test
  void throwsWhenPageIsBeyondLastFloor() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.getFarmSpace(USER_ID, CardType.SEA, 1));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
    verifyNoInteractions(farmSpaceRepository, animalQueryService);
  }

  @Test
  void looksUpAnimalsByFloorRangeOfThePage() {
    givenSpace(FarmSpace.FIRST_FLOOR);
    givenAnimals(Map.of());

    service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    verify(animalQueryService)
        .getByFloorRange(SPACE_ID, FarmSpace.FIRST_FLOOR, LAST_FLOOR_OF_FIRST_PAGE);
  }

  @Test
  void marksFloorsAboveUnlockedFloorAsLocked() {
    givenSpace(FarmSpace.FIRST_FLOOR);
    givenAnimals(Map.of());

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
    givenSpace(2);
    givenAnimals(
        Map.of(
            new AnimalPosition(1, 1), animal(11L, "첫번째", "https://cdn.example.com/a.png"),
            new AnimalPosition(1, 2), animal(22L, "두번째", null)));

    FarmSpaceResponse response = service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    FarmFloorResponse firstFloor = response.floors().get(0);
    assertEquals(FarmSpace.SLOT_COUNT_PER_FLOOR, firstFloor.slots().size());
    assertEquals(1, firstFloor.slots().get(0).slotNum());
    assertEquals("첫번째", firstFloor.slots().get(0).animal().animalName());
    assertEquals(
        "https://cdn.example.com/a.png", firstFloor.slots().get(0).animal().cardImageUrl());
    assertEquals(2, firstFloor.slots().get(1).slotNum());
    assertEquals(22L, firstFloor.slots().get(1).animal().animalId());
    assertNull(firstFloor.slots().get(1).animal().cardImageUrl());

    assertNull(firstFloor.slots().get(2).animal());
    assertNull(firstFloor.slots().get(3).animal());

    FarmFloorResponse secondFloor = response.floors().get(1);
    assertTrue(secondFloor.unlocked());
    assertEquals(FarmSpace.SLOT_COUNT_PER_FLOOR, secondFloor.slots().size());
    assertNull(secondFloor.slots().get(0).animal());
  }

  @Test
  void leavesSlotEmptyWhenNoAnimalAtPosition() {
    givenSpace(FarmSpace.FIRST_FLOOR);
    givenAnimals(Map.of(new AnimalPosition(1, 2), animal(11L, "첫번째", null)));

    FarmSpaceResponse response = service.getFarmSpace(USER_ID, CardType.SEA, FarmSpace.FIRST_PAGE);

    List<FarmSlotResponse> slots = response.floors().get(0).slots();
    assertNull(slots.get(0).animal());
    assertEquals(11L, slots.get(1).animal().animalId());
  }

  private void givenSpace(int unlockedFloor) {
    FarmSpace space = FarmSpace.create(USER_ID, CardType.SEA);
    setField(space, "id", SPACE_ID);
    setField(space, "floor", unlockedFloor);
    given(farmSpaceRepository.findByUserIdAndType(USER_ID, CardType.SEA))
        .willReturn(Optional.of(space));
  }

  private void givenAnimals(Map<AnimalPosition, AnimalResponse> animals) {
    given(animalQueryService.getByFloorRange(anyLong(), anyInt(), anyInt())).willReturn(animals);
  }

  private AnimalResponse animal(Long animalId, String animalName, String cardImage) {
    return new AnimalResponse(animalId, animalName, CardType.SEA, Tier.A, cardImage, null, null);
  }
}
