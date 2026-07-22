package com.somagochi.pochakfarm.farm.infrastructure.persistence;

import com.somagochi.pochakfarm.farm.domain.FarmFloor;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmFloorRepository extends JpaRepository<FarmFloor, Long> {

  List<FarmFloor> findAllBySpaceId(Long spaceId, Pageable pageable);
}
