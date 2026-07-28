package com.somagochi.pochakfarm.farm.infrastructure.persistence;

import com.somagochi.pochakfarm.farm.domain.FarmSlot;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmSlotRepository extends JpaRepository<FarmSlot, Long> {

  List<FarmSlot> findAllByFloorIdInOrderBySequenceAsc(Collection<Long> floorIds);
}
