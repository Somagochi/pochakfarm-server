package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
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
  private final CaptureRepository captureRepository;
  private final FarmQueryService farmQueryService;

  @Transactional(readOnly = true)
  public void validateHasEmptySlot(Long userId, CardType cardType) {
    FarmSpace space = farmQueryService.getSpace(userId, cardType);
    if (findFirstEmptyPosition(space).isEmpty()) {
      throw new BusinessException(ErrorCode.FARM_SPACE_FULL);
    }
  }

  @Transactional
  public Animal placeInFirstAvailableSlot(Long userId, CardType cardType, Long captureId) {
    FarmSpace space = farmQueryService.getSpaceForUpdate(userId, cardType);
    AnimalPosition position =
        findFirstEmptyPosition(space)
            .orElseThrow(() -> new BusinessException(ErrorCode.FARM_SPACE_FULL));
    return animalRepository.save(
        Animal.create(captureId, space.getId(), position.floorNum(), position.slotNum()));
  }

  @Transactional
  public Animal placeAt(
      Long userId, CardType cardType, Long captureId, Integer floorNum, Integer slotNum) {
    FarmSpace space = validatedSpace(userId, cardType, floorNum, slotNum);
    if (findOccupant(space, floorNum, slotNum).isPresent()) {
      throw new BusinessException(ErrorCode.FARM_SLOT_OCCUPIED);
    }
    return animalRepository.save(Animal.create(captureId, space.getId(), floorNum, slotNum));
  }

  @Transactional
  public Animal replaceAt(
      Long userId,
      CardType cardType,
      Long captureId,
      Long replacedAnimalId,
      Integer floorNum,
      Integer slotNum) {
    FarmSpace space = validatedSpace(userId, cardType, floorNum, slotNum);
    Animal occupant =
        findOccupant(space, floorNum, slotNum)
            .filter(animal -> animal.getId().equals(replacedAnimalId))
            .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_REPLACEMENT_CONFLICT));
    validateReplacedAnimal(userId, cardType, occupant);
    animalRepository.delete(occupant);
    animalRepository.flush();
    return animalRepository.save(Animal.create(captureId, space.getId(), floorNum, slotNum));
  }

  private FarmSpace validatedSpace(
      Long userId, CardType cardType, Integer floorNum, Integer slotNum) {
    FarmSpace space = farmQueryService.getSpaceForUpdate(userId, cardType);
    if (!space.canPlaceAt(floorNum, slotNum)) {
      throw new BusinessException(ErrorCode.FARM_SLOT_NOT_FOUND);
    }
    return space;
  }

  private Optional<Animal> findOccupant(FarmSpace space, Integer floorNum, Integer slotNum) {
    return animalRepository.findBySpaceIdAndFloorNumAndSlotNumForUpdate(
        space.getId(), floorNum, slotNum);
  }

  private void validateReplacedAnimal(Long userId, CardType cardType, Animal occupant) {
    Capture capture =
        captureRepository
            .findById(occupant.getCaptureId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    if (!capture.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_ANIMAL_ACCESS);
    }
    if (capture.getCardType() != cardType) {
      throw new BusinessException(ErrorCode.FARM_SLOT_TYPE_MISMATCH);
    }
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
