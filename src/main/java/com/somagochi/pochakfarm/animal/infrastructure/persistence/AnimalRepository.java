package com.somagochi.pochakfarm.animal.infrastructure.persistence;

import com.somagochi.pochakfarm.animal.domain.Animal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

  List<Animal> findByCaptureIdInAndIdLessThanOrderByIdDesc(
      Collection<Long> captureIds, Long cursor, Pageable pageable);

  List<Animal> findBySlotIdIn(Collection<Long> slotIds);
}
