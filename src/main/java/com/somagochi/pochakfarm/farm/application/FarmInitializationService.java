package com.somagochi.pochakfarm.farm.application;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.farm.domain.FarmFloor;
import com.somagochi.pochakfarm.farm.domain.FarmSlot;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmFloorRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSlotRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FarmInitializationService {

  private final FarmSpaceRepository farmSpaceRepository;
  private final FarmFloorRepository farmFloorRepository;
  private final FarmSlotRepository farmSlotRepository;

  public FarmInitializationService(
      FarmSpaceRepository farmSpaceRepository,
      FarmFloorRepository farmFloorRepository,
      FarmSlotRepository farmSlotRepository) {
    this.farmSpaceRepository = farmSpaceRepository;
    this.farmFloorRepository = farmFloorRepository;
    this.farmSlotRepository = farmSlotRepository;
  }

  public void initialize(Long userId) {
    for (CardType type : CardType.values()) {
      createSpaceWithFirstFloor(userId, type);
    }
  }

  private void createSpaceWithFirstFloor(Long userId, CardType type) {
    FarmSpace space = farmSpaceRepository.save(FarmSpace.create(userId, type));
    FarmFloor floor =
        farmFloorRepository.save(FarmFloor.create(space.getId(), FarmFloor.FIRST_SEQUENCE));
    farmSlotRepository.saveAll(createEmptySlots(floor.getId()));
  }

  private List<FarmSlot> createEmptySlots(Long floorId) {
    List<FarmSlot> slots = new ArrayList<>();
    for (int sequence = 1; sequence <= FarmFloor.SLOT_COUNT_PER_FLOOR; sequence++) {
      slots.add(FarmSlot.create(floorId, sequence));
    }
    return slots;
  }
}
