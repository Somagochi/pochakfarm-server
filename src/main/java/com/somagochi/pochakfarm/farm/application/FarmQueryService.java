package com.somagochi.pochakfarm.farm.application;

import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.domain.FarmFloorRange;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.dto.FarmAnimalResponse;
import com.somagochi.pochakfarm.farm.dto.FarmFloorResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSlotResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSpaceResponse;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmQueryService {

  private final FarmSpaceRepository farmSpaceRepository;
  private final AnimalQueryService animalQueryService;

  public FarmQueryService(
      FarmSpaceRepository farmSpaceRepository, AnimalQueryService animalQueryService) {
    this.farmSpaceRepository = farmSpaceRepository;
    this.animalQueryService = animalQueryService;
  }

  @Transactional(readOnly = true)
  public FarmSpaceResponse getFarmSpace(Long userId, CardType type, int page) {
    FarmFloorRange range = FarmFloorRange.ofPage(page);
    FarmSpace space = findSpace(userId, type);
    Map<AnimalPosition, AnimalResponse> animals =
        animalQueryService.getByFloorRange(
            space.getId(), range.firstSequence(), range.lastSequence());
    return FarmSpaceResponse.of(space.getType(), range, toFloors(space, range, animals));
  }

  private FarmSpace findSpace(Long userId, CardType type) {
    return farmSpaceRepository
        .findByUserIdAndType(userId, type)
        .orElseThrow(() -> new BusinessException(ErrorCode.FARM_SPACE_NOT_FOUND));
  }

  private List<FarmFloorResponse> toFloors(
      FarmSpace space, FarmFloorRange range, Map<AnimalPosition, AnimalResponse> animals) {
    List<FarmFloorResponse> floors = new ArrayList<>();
    for (int sequence = range.firstSequence(); sequence <= range.lastSequence(); sequence++) {
      floors.add(
          space.isUnlocked(sequence)
              ? FarmFloorResponse.unlocked(sequence, toSlots(sequence, animals))
              : FarmFloorResponse.locked(sequence));
    }
    return floors;
  }

  private List<FarmSlotResponse> toSlots(
      int floorSequence, Map<AnimalPosition, AnimalResponse> animals) {
    List<FarmSlotResponse> slots = new ArrayList<>();
    for (int sequence = 1; sequence <= FarmSpace.SLOT_COUNT_PER_FLOOR; sequence++) {
      AnimalResponse animal = animals.get(new AnimalPosition(floorSequence, sequence));
      slots.add(
          animal == null
              ? FarmSlotResponse.empty(sequence)
              : FarmSlotResponse.occupied(sequence, toAnimal(animal)));
    }
    return slots;
  }

  private FarmAnimalResponse toAnimal(AnimalResponse animal) {
    return new FarmAnimalResponse(
        animal.animalId(), animal.animalName(), animal.cardImageUrl(), animal.animalImageUrl());
  }
}
