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
import java.util.Optional;
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
  public AnimalSlotMoveResponse moveToSlot(Long userId, Long animalId, Long targetSlotId) {
    Animal animal =
        animalRepository
            .findById(animalId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    Capture capture = findOwnedCapture(userId, animal.getCaptureId());
    if (targetSlotId.equals(animal.getSlotId())) {
      return new AnimalSlotMoveResponse(animal.getId(), animal.getSlotId());
    }
    FarmSpace targetSpace = findOwnedSpace(userId, targetSlotId);
    if (targetSpace.getType() != capture.getCardType()) {
      throw new BusinessException(ErrorCode.FARM_SLOT_TYPE_MISMATCH);
    }
    Long sourceSlotId = animal.getSlotId();
    Optional<Animal> occupant = animalRepository.findBySlotId(targetSlotId);
    occupant.ifPresent(other -> other.moveTo(sourceSlotId));
    animal.moveTo(targetSlotId);
    return new AnimalSlotMoveResponse(animal.getId(), targetSlotId);
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

  private FarmSpace findOwnedSpace(Long userId, Long targetSlotId) {
    FarmSpace space =
        farmQueryService
            .findSpaceBySlotId(targetSlotId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FARM_SLOT_NOT_FOUND));
    if (!space.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_FARM_SLOT_ACCESS);
    }
    return space;
  }
}
