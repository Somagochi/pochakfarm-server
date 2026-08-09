package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.animal.application.AnimalPlacementService;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaptureAnimalService {

  private final CaptureRepository captureRepository;
  private final AnimalRepository animalRepository;
  private final AnimalPlacementService animalPlacementService;
  private final FileStorage fileStorage;

  @Transactional
  public CaptureAnimalPlacementResponse place(
      Long userId, Long captureId, CaptureAnimalPlacementRequest request) {
    if (request == null) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    Capture capture = findCaptureForUpdate(captureId);
    validateOwner(capture, userId);
    validatePlaceable(capture);

    Optional<Animal> existing = animalRepository.findByCaptureId(captureId);
    if (existing.isPresent()) {
      return existingResponse(capture, existing.get(), request);
    }

    Animal animal =
        request.replacedAnimalId() == null
            ? animalPlacementService.placeAt(
                userId, capture.getCardType(), captureId, request.floorNum(), request.slotNum())
            : animalPlacementService.replaceAt(
                userId,
                capture.getCardType(),
                captureId,
                request.replacedAnimalId(),
                request.floorNum(),
                request.slotNum());
    return response(capture, animal);
  }

  private CaptureAnimalPlacementResponse existingResponse(
      Capture capture, Animal animal, CaptureAnimalPlacementRequest request) {
    if (!animal.isAt(animal.getSpaceId(), request.floorNum(), request.slotNum())) {
      throw new BusinessException(ErrorCode.CAPTURE_PLACEMENT_CONFLICT);
    }
    return response(capture, animal);
  }

  private CaptureAnimalPlacementResponse response(Capture capture, Animal animal) {
    return new CaptureAnimalPlacementResponse(
        animal.getId(),
        capture.getId(),
        capture.getCardType(),
        animal.getFloorNum(),
        animal.getSlotNum(),
        fileStorage.buildUrl(capture.getAnimalImage()));
  }

  private Capture findCaptureForUpdate(Long captureId) {
    return captureRepository
        .findByIdForUpdate(captureId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CAPTURE_NOT_FOUND));
  }

  private void validateOwner(Capture capture, Long userId) {
    if (!capture.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_CAPTURE_ACCESS);
    }
  }

  private void validatePlaceable(Capture capture) {
    if (!capture.isPlaceableInFarm() || capture.getAnimalImage() == null) {
      throw new BusinessException(ErrorCode.CAPTURE_NOT_PLACEABLE);
    }
  }
}
