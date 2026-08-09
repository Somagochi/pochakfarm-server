package com.somagochi.pochakfarm.farm.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FarmInitializationServiceTest {

  private static final Long USER_ID = 1L;

  private final FarmSpaceRepository farmSpaceRepository = mock(FarmSpaceRepository.class);
  private final FarmInitializationService service =
      new FarmInitializationService(farmSpaceRepository);

  @Test
  void createsSpaceForEveryCardType() {
    service.initialize(USER_ID);

    List<FarmSpace> spaces = savedSpaces();
    assertEquals(CardType.values().length, spaces.size());
    assertEquals(
        Set.of(CardType.values()),
        spaces.stream().map(FarmSpace::getType).collect(Collectors.toSet()));
    spaces.forEach(space -> assertEquals(USER_ID, space.getUserId()));
  }

  @Test
  void unlocksFirstFloorOnCreation() {
    service.initialize(USER_ID);

    savedSpaces().forEach(space -> assertEquals(FarmSpace.FIRST_FLOOR, space.getFloor()));
  }

  @SuppressWarnings("unchecked")
  private List<FarmSpace> savedSpaces() {
    ArgumentCaptor<List<FarmSpace>> captor = ArgumentCaptor.forClass(List.class);
    verify(farmSpaceRepository).saveAll(captor.capture());
    return captor.getValue();
  }
}
