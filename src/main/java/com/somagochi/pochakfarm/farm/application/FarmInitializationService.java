package com.somagochi.pochakfarm.farm.application;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FarmInitializationService {

  private final FarmSpaceRepository farmSpaceRepository;

  public FarmInitializationService(FarmSpaceRepository farmSpaceRepository) {
    this.farmSpaceRepository = farmSpaceRepository;
  }

  public void initialize(Long userId) {
    List<FarmSpace> spaces =
        Arrays.stream(CardType.values()).map(type -> FarmSpace.create(userId, type)).toList();
    farmSpaceRepository.saveAll(spaces);
  }
}
