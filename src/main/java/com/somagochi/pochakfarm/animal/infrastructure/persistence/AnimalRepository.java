package com.somagochi.pochakfarm.animal.infrastructure.persistence;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

  Optional<Animal> findByCaptureId(Long captureId);

  @Query(
      "select a from Animal a, FarmSpace s "
          + "where a.spaceId = s.id and s.userId = :userId and s.type in :cardTypes "
          + "and a.id < :cursor "
          + "order by a.id desc")
  List<Animal> findOwnedAnimals(
      @Param("userId") Long userId,
      @Param("cardTypes") Collection<CardType> cardTypes,
      @Param("cursor") Long cursor,
      Limit limit);

  List<Animal> findBySpaceIdAndFloorNumBetween(
      Long spaceId, Integer firstFloorNum, Integer lastFloorNum);

  Optional<Animal> findBySpaceIdAndFloorNumAndSlotNum(
      Long spaceId, Integer floorNum, Integer slotNum);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select a from Animal a "
          + "where a.spaceId = :spaceId and a.floorNum = :floorNum and a.slotNum = :slotNum")
  Optional<Animal> findBySpaceIdAndFloorNumAndSlotNumForUpdate(
      @Param("spaceId") Long spaceId,
      @Param("floorNum") Integer floorNum,
      @Param("slotNum") Integer slotNum);

  @Query(
      "select a from Animal a, Capture c "
          + "where a.id = :animalId and a.captureId = c.id and c.userId = :userId")
  Optional<Animal> findOwnedAnimal(@Param("userId") Long userId, @Param("animalId") Long animalId);
}
