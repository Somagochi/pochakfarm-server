package com.somagochi.pochakfarm.animal.infrastructure.persistence;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalBattleProfile;
import com.somagochi.pochakfarm.animal.dto.AnimalTypeCount;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

  Optional<Animal> findByCaptureId(Long captureId);

  List<Animal> findByCaptureIdIn(Collection<Long> captureIds);

  @Modifying(clearAutomatically = true)
  @Query(
      "update Animal a "
          + "set a.restEndsAt = :restEndsAt, a.version = a.version + 1, a.updatedAt = :now "
          + "where a.id = :animalId "
          + "and (a.restEndsAt is null or a.restEndsAt <= :now)")
  int reserveRest(
      @Param("animalId") Long animalId,
      @Param("restEndsAt") Instant restEndsAt,
      @Param("now") Instant now);

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

  @Query(
      "select new com.somagochi.pochakfarm.animal.dto.AnimalBattleProfile("
          + "a.id, c.id, c.animalName.value, c.cardType, c.tier, c.skill1, c.skill2) "
          + "from Animal a, Capture c "
          + "where a.captureId = c.id and c.userId = :userId and a.id in :animalIds")
  List<AnimalBattleProfile> findOwnedBattleProfiles(
      @Param("userId") Long userId, @Param("animalIds") Collection<Long> animalIds);

  @Query(
      "select new com.somagochi.pochakfarm.animal.dto.AnimalTypeCount(s.type, count(a)) "
          + "from Animal a, FarmSpace s "
          + "where a.spaceId = s.id and s.userId = :userId "
          + "group by s.type")
  List<AnimalTypeCount> countOwnedByCardType(@Param("userId") Long userId);

  @Query(
      "select a from Animal a, FarmSpace s "
          + "where a.spaceId = s.id and s.userId = :userId and a.floorNum >= 1")
  List<Animal> findPlacedAnimals(@Param("userId") Long userId);

  @Query(
      "select a from Animal a, Capture c "
          + "where a.captureId = c.id and c.userId = :userId "
          + "and c.cardType in :cardTypes "
          + "and c.animalName.value like :keyword escape '!' "
          + "and a.id < :cursor "
          + "order by a.id desc")
  List<Animal> searchOwnedAnimalsByName(
      @Param("userId") Long userId,
      @Param("cardTypes") Collection<CardType> cardTypes,
      @Param("keyword") String keyword,
      @Param("cursor") Long cursor,
      Limit limit);
}
