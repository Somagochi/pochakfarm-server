package com.somagochi.pochakfarm.animal.infrastructure.persistence;

import com.somagochi.pochakfarm.animal.domain.Animal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

  List<Animal> findBySlotIdIn(Collection<Long> slotIds);

  Optional<Animal> findBySlotId(Long slotId);
}
