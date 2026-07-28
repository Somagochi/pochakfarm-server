package com.somagochi.pochakfarm.animal.infrastructure.persistence;

import com.somagochi.pochakfarm.animal.domain.Animal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

  List<Animal> findByCaptureIdInAndIdLessThanOrderByIdDesc(
      Collection<Long> captureIds, Long cursor, Pageable pageable);

  List<Animal> findBySlotIdIn(Collection<Long> slotIds);

  @Query(
      "select a from Animal a, Capture c "
          + "where a.id = :animalId and a.captureId = c.id and c.userId = :userId")
  Optional<Animal> findOwnedAnimal(@Param("userId") Long userId, @Param("animalId") Long animalId);

  Optional<Animal> findBySlotId(Long slotId);
}
