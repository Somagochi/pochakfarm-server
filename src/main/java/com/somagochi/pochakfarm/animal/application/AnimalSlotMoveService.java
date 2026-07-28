package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalSlotMoveResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.application.CaptureQueryService;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalSlotMoveService {

  private final AnimalRepository animalRepository;
  private final CaptureQueryService captureQueryService;
  private final FarmQueryService farmQueryService;

  public AnimalSlotMoveService(
      AnimalRepository animalRepository,
      CaptureQueryService captureQueryService,
      FarmQueryService farmQueryService) {
    this.animalRepository = animalRepository;
    this.captureQueryService = captureQueryService;
    this.farmQueryService = farmQueryService;
  }

  @Transactional
  public AnimalSlotMoveResponse moveToSlot(
      Long userId, Long animalId, Integer targetFloorNum, Integer targetSlotNum) {
    Animal animal =
        animalRepository
            .findById(animalId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    Capture capture = findOwnedCapture(userId, animal.getCaptureId());
    FarmSpace space = farmQueryService.getSpace(userId, capture.getCardType());
    if (!space.canPlaceAt(targetFloorNum, targetSlotNum)) {
      throw new BusinessException(ErrorCode.FARM_SLOT_NOT_FOUND);
    }
    if (animal.isAt(space.getId(), targetFloorNum, targetSlotNum)) {
      return new AnimalSlotMoveResponse(animal.getId(), targetFloorNum, targetSlotNum);
    }
    swapOccupant(animal, space.getId(), targetFloorNum, targetSlotNum);
    animal.moveTo(space.getId(), targetFloorNum, targetSlotNum);
    return new AnimalSlotMoveResponse(animal.getId(), targetFloorNum, targetSlotNum);
  }

  private void swapOccupant(
      Animal animal, Long spaceId, Integer targetFloorNum, Integer targetSlotNum) {
    Long sourceSpaceId = animal.getSpaceId();
    Integer sourceFloorNum = animal.getFloorNum();
    Integer sourceSlotNum = animal.getSlotNum();
    animalRepository
        .findBySpaceIdAndFloorNumAndSlotNum(spaceId, targetFloorNum, targetSlotNum)
        .ifPresent(occupant -> occupant.moveTo(sourceSpaceId, sourceFloorNum, sourceSlotNum));
  }

  private Capture findOwnedCapture(Long userId, Long captureId) {
    Capture capture =
        captureQueryService
            .findById(captureId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    if (!capture.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_ANIMAL_ACCESS);
    }
    return capture;
  }
}
