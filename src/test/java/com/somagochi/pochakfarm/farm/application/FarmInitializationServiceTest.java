package com.somagochi.pochakfarm.farm.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.farm.domain.FarmFloor;
import com.somagochi.pochakfarm.farm.domain.FarmSlot;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmFloorRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSlotRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FarmInitializationServiceTest {

  private static final Long USER_ID = 1L;
  private static final int CARD_TYPE_COUNT = CardType.values().length;

  private final FarmSpaceRepository farmSpaceRepository = mock(FarmSpaceRepository.class);
  private final FarmFloorRepository farmFloorRepository = mock(FarmFloorRepository.class);
  private final FarmSlotRepository farmSlotRepository = mock(FarmSlotRepository.class);
  private final FarmInitializationService service =
      new FarmInitializationService(farmSpaceRepository, farmFloorRepository, farmSlotRepository);

  @Test
  void createsSpaceWithFirstFloorForEveryCardType() {
    stubSaves();

    service.initialize(USER_ID);

    ArgumentCaptor<FarmSpace> spaceCaptor = ArgumentCaptor.forClass(FarmSpace.class);
    verify(farmSpaceRepository, times(CARD_TYPE_COUNT)).save(spaceCaptor.capture());
    assertEquals(
        Set.of(CardType.values()),
        spaceCaptor.getAllValues().stream().map(FarmSpace::getType).collect(Collectors.toSet()));
    spaceCaptor.getAllValues().forEach(space -> assertEquals(USER_ID, space.getUserId()));
  }

  @Test
  void createsFirstFloorWithFourEmptySlots() {
    stubSaves();

    service.initialize(USER_ID);

    ArgumentCaptor<FarmFloor> floorCaptor = ArgumentCaptor.forClass(FarmFloor.class);
    verify(farmFloorRepository, times(CARD_TYPE_COUNT)).save(floorCaptor.capture());
    floorCaptor
        .getAllValues()
        .forEach(floor -> assertEquals(FarmFloor.FIRST_SEQUENCE, floor.getSequence()));

    ArgumentCaptor<List<FarmSlot>> slotsCaptor = ArgumentCaptor.forClass(List.class);
    verify(farmSlotRepository, times(CARD_TYPE_COUNT)).saveAll(slotsCaptor.capture());
    slotsCaptor
        .getAllValues()
        .forEach(
            slots -> {
              assertEquals(FarmFloor.SLOT_COUNT_PER_FLOOR, slots.size());
              assertEquals(
                  List.of(1, 2, 3, 4),
                  slots.stream().map(FarmSlot::getSequence).collect(Collectors.toList()));
            });
  }

  private void stubSaves() {
    given(farmSpaceRepository.save(any()))
        .willAnswer(invocation -> withId(invocation.getArgument(0), 100L));
    given(farmFloorRepository.save(any()))
        .willAnswer(invocation -> withId(invocation.getArgument(0), 1000L));
  }

  private static Object withId(Object entity, Long id) {
    setField(entity, "id", id);
    return entity;
  }
}
