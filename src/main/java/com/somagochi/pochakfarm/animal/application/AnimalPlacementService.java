package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnimalPlacementService {

  private final AnimalRepository animalRepository;
  private final FarmQueryService farmQueryService;

  @Transactional(readOnly = true)
  public void validateHasEmptySlot(Long userId, CardType cardType) {
    FarmSpace space = farmQueryService.getSpace(userId, cardType);
    if (findFirstEmptyPosition(space).isEmpty()) {
      throw new BusinessException(ErrorCode.FARM_SPACE_FULL);
    }
  }

  @Transactional
  public Animal place(Long userId, CardType cardType, Long captureId) {
    FarmSpace space = farmQueryService.getSpaceForUpdate(userId, cardType);
    AnimalPosition position =
        findFirstEmptyPosition(space)
            .orElseThrow(() -> new BusinessException(ErrorCode.FARM_SPACE_FULL));
    return animalRepository.save(
        Animal.create(captureId, space.getId(), position.floorNum(), position.slotNum()));
  }

  private Optional<AnimalPosition> findFirstEmptyPosition(FarmSpace space) {
    Set<AnimalPosition> occupied =
        animalRepository
            .findBySpaceIdAndFloorNumBetween(space.getId(), FarmSpace.FIRST_FLOOR, space.getFloor())
            .stream()
            .map(animal -> new AnimalPosition(animal.getFloorNum(), animal.getSlotNum()))
            .collect(Collectors.toSet());
    for (int floorNum = FarmSpace.FIRST_FLOOR; floorNum <= space.getFloor(); floorNum++) {
      for (int slotNum = FarmSpace.FIRST_SLOT;
          slotNum <= FarmSpace.SLOT_COUNT_PER_FLOOR;
          slotNum++) {
        AnimalPosition position = new AnimalPosition(floorNum, slotNum);
        if (!occupied.contains(position)) {
          return Optional.of(position);
        }
      }
    }
    return Optional.empty();
  }
}
