package com.somagochi.pochakfarm.farm.application;

import com.somagochi.pochakfarm.animal.application.AnimalReadService;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.farm.domain.FarmFloor;
import com.somagochi.pochakfarm.farm.domain.FarmFloorRange;
import com.somagochi.pochakfarm.farm.domain.FarmSlot;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.dto.FarmAnimalResponse;
import com.somagochi.pochakfarm.farm.dto.FarmFloorResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSlotResponse;
import com.somagochi.pochakfarm.farm.dto.FarmSpaceResponse;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmFloorRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSlotRepository;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmQueryService {

  private final FarmSpaceRepository farmSpaceRepository;
  private final FarmFloorRepository farmFloorRepository;
  private final FarmSlotRepository farmSlotRepository;
  private final AnimalReadService animalReadService;

  public FarmQueryService(
      FarmSpaceRepository farmSpaceRepository,
      FarmFloorRepository farmFloorRepository,
      FarmSlotRepository farmSlotRepository,
      AnimalReadService animalReadService) {
    this.farmSpaceRepository = farmSpaceRepository;
    this.farmFloorRepository = farmFloorRepository;
    this.farmSlotRepository = farmSlotRepository;
    this.animalReadService = animalReadService;
  }

  @Transactional(readOnly = true)
  public FarmSpaceResponse getFarmSpace(Long userId, CardType type, int page) {
    FarmFloorRange range = FarmFloorRange.ofPage(page);
    return toSpaceResponse(findSpace(userId, type), range);
  }

  private FarmSpaceResponse toSpaceResponse(FarmSpace space, FarmFloorRange range) {
    Map<Integer, List<FarmSlot>> slotsByFloorSequence =
        findSlotsByFloorSequence(findUnlockedFloors(space, range));
    return FarmSpaceResponse.of(space.getType(), range, toFloors(range, slotsByFloorSequence));
  }

  private FarmSpace findSpace(Long userId, CardType type) {
    return farmSpaceRepository
        .findByUserIdAndType(userId, type)
        .orElseThrow(() -> new BusinessException(ErrorCode.FARM_SPACE_NOT_FOUND));
  }

  private List<FarmFloor> findUnlockedFloors(FarmSpace space, FarmFloorRange range) {
    return farmFloorRepository.findAllBySpaceId(space.getId(), toPageable(range));
  }

  private Pageable toPageable(FarmFloorRange range) {
    return PageRequest.of(range.page(), range.size(), Sort.by(Sort.Direction.ASC, "sequence"));
  }

  private Map<Integer, List<FarmSlot>> findSlotsByFloorSequence(List<FarmFloor> floors) {
    if (floors.isEmpty()) {
      return Map.of();
    }
    Map<Long, Integer> sequenceByFloorId =
        floors.stream().collect(Collectors.toMap(FarmFloor::getId, FarmFloor::getSequence));
    Map<Integer, List<FarmSlot>> slotsByFloorSequence =
        farmSlotRepository.findAllByFloorIdInOrderBySequenceAsc(sequenceByFloorId.keySet()).stream()
            .collect(Collectors.groupingBy(slot -> sequenceByFloorId.get(slot.getFloorId())));
    floors.forEach(floor -> slotsByFloorSequence.putIfAbsent(floor.getSequence(), List.of()));
    return slotsByFloorSequence;
  }

  private List<FarmFloorResponse> toFloors(
      FarmFloorRange range, Map<Integer, List<FarmSlot>> slotsByFloorSequence) {
    Map<Long, AnimalResponse> animals = findAnimals(slotsByFloorSequence);
    List<FarmFloorResponse> floors = new ArrayList<>();
    for (int sequence = range.firstSequence(); sequence <= range.lastSequence(); sequence++) {
      List<FarmSlot> slots = slotsByFloorSequence.get(sequence);
      floors.add(
          slots == null
              ? FarmFloorResponse.locked(sequence)
              : FarmFloorResponse.unlocked(sequence, toSlots(slots, animals)));
    }
    return floors;
  }

  private Map<Long, AnimalResponse> findAnimals(Map<Integer, List<FarmSlot>> slotsByFloorSequence) {
    Set<Long> animalIds =
        slotsByFloorSequence.values().stream()
            .flatMap(List::stream)
            .map(FarmSlot::getAnimalId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    return animalReadService.getAnimals(animalIds);
  }

  private List<FarmSlotResponse> toSlots(List<FarmSlot> slots, Map<Long, AnimalResponse> animals) {
    Map<Integer, FarmSlot> slotsBySequence =
        slots.stream()
            .collect(
                Collectors.toMap(FarmSlot::getSequence, slot -> slot, (first, second) -> first));
    List<FarmSlotResponse> responses = new ArrayList<>();
    for (int sequence = 1; sequence <= FarmFloor.SLOT_COUNT_PER_FLOOR; sequence++) {
      responses.add(toSlot(sequence, slotsBySequence.get(sequence), animals));
    }
    return responses;
  }

  private FarmSlotResponse toSlot(
      Integer sequence, FarmSlot slot, Map<Long, AnimalResponse> animals) {
    Long slotId = slot == null ? null : slot.getId();
    AnimalResponse animal = slot == null || slot.isEmpty() ? null : animals.get(slot.getAnimalId());
    return animal == null
        ? FarmSlotResponse.empty(slotId, sequence)
        : FarmSlotResponse.occupied(slotId, sequence, toAnimal(animal));
  }

  private FarmAnimalResponse toAnimal(AnimalResponse animal) {
    return new FarmAnimalResponse(
        animal.animalId(), animal.animalName(), animal.cardImageUrl(), animal.animalImageUrl());
  }
}
