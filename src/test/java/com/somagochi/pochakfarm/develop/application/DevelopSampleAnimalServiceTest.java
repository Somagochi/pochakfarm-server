package com.somagochi.pochakfarm.develop.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardMetadataGenerator;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import com.somagochi.pochakfarm.develop.dto.DevelopSampleAnimalResponse;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DevelopSampleAnimalServiceTest {

  private static final Long USER_ID = 1L;
  private static final int UNLOCKED_FLOOR = 3;

  private final FarmQueryService farmQueryService = mock(FarmQueryService.class);
  private final AnimalQueryService animalQueryService = mock(AnimalQueryService.class);
  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final CaptureRepository captureRepository = mock(CaptureRepository.class);
  private final RandomProvider randomProvider = mock(RandomProvider.class);
  private final DevelopSampleAnimalService service =
      new DevelopSampleAnimalService(
          farmQueryService,
          animalQueryService,
          animalRepository,
          captureRepository,
          new CardMetadataGenerator(),
          randomProvider);

  private final Map<Long, CardType> cardTypeBySpaceId = new HashMap<>();

  @BeforeEach
  void assignIdsOnSave() {
    AtomicLong captureIds = new AtomicLong();
    given(captureRepository.save(any(Capture.class)))
        .willAnswer(
            invocation -> {
              Capture capture = invocation.getArgument(0);
              setField(capture, "id", captureIds.incrementAndGet());
              return capture;
            });
    given(randomProvider.nextInt(anyInt()))
        .willAnswer(invocation -> invocation.getArgument(0, Integer.class) - 1);
  }

  @Test
  void placesAnimalsOnlyWithinUnlockedFloors() {
    givenSpaces(UNLOCKED_FLOOR);
    givenNoOccupiedPositions();

    service.createSampleAnimal(USER_ID);

    for (Animal animal : savedAnimals()) {
      assertTrue(animal.getFloorNum() >= FarmSpace.FIRST_FLOOR);
      assertTrue(animal.getFloorNum() <= UNLOCKED_FLOOR);
      assertTrue(animal.getSlotNum() >= FarmSpace.FIRST_SLOT);
      assertTrue(animal.getSlotNum() <= FarmSpace.SLOT_COUNT_PER_FLOOR);
    }
  }

  @Test
  void neverPlacesTwoAnimalsOnTheSamePosition() {
    givenSpaces(UNLOCKED_FLOOR);
    givenNoOccupiedPositions();

    service.createSampleAnimal(USER_ID);

    List<Animal> animals = savedAnimals();
    Set<String> positions = new HashSet<>();
    for (Animal animal : animals) {
      positions.add(animal.getSpaceId() + ":" + animal.getFloorNum() + ":" + animal.getSlotNum());
    }
    assertEquals(animals.size(), positions.size());
  }

  @Test
  void skipsAlreadyOccupiedPositions() {
    givenSpaces(FarmSpace.FIRST_FLOOR);
    AnimalPosition occupied = new AnimalPosition(FarmSpace.FIRST_FLOOR, FarmSpace.FIRST_SLOT);
    givenOccupiedPositions(occupied);

    service.createSampleAnimal(USER_ID);

    List<Animal> animals = savedAnimals();
    assertFalse(animals.isEmpty());
    for (Animal animal : animals) {
      assertFalse(new AnimalPosition(animal.getFloorNum(), animal.getSlotNum()).equals(occupied));
    }
  }

  @Test
  void fillsTheLastRemainingSlot() {
    givenSpaces(FarmSpace.FIRST_FLOOR);
    givenOccupiedPositions(
        new AnimalPosition(1, 1), new AnimalPosition(1, 2), new AnimalPosition(1, 3));

    DevelopSampleAnimalResponse response = service.createSampleAnimal(USER_ID);

    assertEquals(CardType.values().length, response.createdCount());
    for (Animal animal : savedAnimals()) {
      assertEquals(4, animal.getSlotNum());
    }
  }

  @Test
  void createsNothingWhenEveryFarmIsFull() {
    givenSpaces(FarmSpace.FIRST_FLOOR);
    givenOccupiedPositions(
        new AnimalPosition(1, 1),
        new AnimalPosition(1, 2),
        new AnimalPosition(1, 3),
        new AnimalPosition(1, 4));

    DevelopSampleAnimalResponse response = service.createSampleAnimal(USER_ID);

    assertEquals(0, response.createdCount());
    verify(animalRepository, never()).save(any(Animal.class));
    verify(captureRepository, never()).save(any(Capture.class));
  }

  @Test
  void reportsHowManyAnimalsWereCreated() {
    givenSpaces(UNLOCKED_FLOOR);
    givenNoOccupiedPositions();

    DevelopSampleAnimalResponse response = service.createSampleAnimal(USER_ID);

    assertEquals(savedAnimals().size(), response.createdCount());
  }

  @Test
  void createsCaptureMatchingTheCardTypeOfItsFarm() {
    givenSpaces(UNLOCKED_FLOOR);
    givenNoOccupiedPositions();

    service.createSampleAnimal(USER_ID);

    List<Capture> captures = savedCaptures();
    List<Animal> animals = savedAnimals();
    assertEquals(captures.size(), animals.size());
    for (int index = 0; index < animals.size(); index++) {
      Animal animal = animals.get(index);
      Capture capture = captures.get(index);
      assertEquals(cardTypeBySpaceId.get(animal.getSpaceId()), capture.getCardType());
      assertEquals(capture.getId(), animal.getCaptureId());
      assertEquals(USER_ID, capture.getUserId());
      assertEquals(capture.getCardType(), capture.getSkill1().cardType());
      assertEquals(capture.getCardType(), capture.getSkill2().cardType());
      assertFalse(capture.getSkill1() == capture.getSkill2());
    }
  }

  @Test
  void propagatesWhenFarmSpaceIsMissing() {
    given(farmQueryService.getSpace(anyLong(), any(CardType.class)))
        .willThrow(new BusinessException(ErrorCode.FARM_SPACE_NOT_FOUND));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.createSampleAnimal(USER_ID));

    assertEquals(ErrorCode.FARM_SPACE_NOT_FOUND.getCode(), exception.getCode());
    verify(animalRepository, never()).save(any(Animal.class));
  }

  private void givenSpaces(int unlockedFloor) {
    long spaceId = 100L;
    for (CardType cardType : CardType.values()) {
      FarmSpace space = FarmSpace.create(USER_ID, cardType);
      setField(space, "id", spaceId);
      setField(space, "floor", unlockedFloor);
      cardTypeBySpaceId.put(spaceId, cardType);
      given(farmQueryService.getSpace(USER_ID, cardType)).willReturn(space);
      spaceId++;
    }
  }

  private void givenNoOccupiedPositions() {
    given(animalQueryService.getByFloorRange(anyLong(), anyInt(), anyInt())).willReturn(Map.of());
  }

  private void givenOccupiedPositions(AnimalPosition... positions) {
    Map<AnimalPosition, AnimalResponse> occupied = new HashMap<>();
    for (AnimalPosition position : positions) {
      occupied.put(position, null);
    }
    given(animalQueryService.getByFloorRange(anyLong(), anyInt(), anyInt())).willReturn(occupied);
  }

  private List<Animal> savedAnimals() {
    ArgumentCaptor<Animal> captor = ArgumentCaptor.forClass(Animal.class);
    verify(animalRepository, atLeastOnce()).save(captor.capture());
    return new ArrayList<>(captor.getAllValues());
  }

  private List<Capture> savedCaptures() {
    ArgumentCaptor<Capture> captor = ArgumentCaptor.forClass(Capture.class);
    verify(captureRepository, atLeastOnce()).save(captor.capture());
    return new ArrayList<>(captor.getAllValues());
  }
}
