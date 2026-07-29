package com.somagochi.pochakfarm.animal.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "animals")
@SQLRestriction("deleted_at is null")
public class Animal extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "capture_id")
  private Long captureId;

  @Column(name = "space_id")
  private Long spaceId;

  @Column(name = "floor_num")
  private Integer floorNum = 0;

  @Column(name = "slot_num")
  private Integer slotNum = 0;

  private Animal(Long captureId, Long spaceId, Integer floorNum, Integer slotNum) {
    this.captureId = Objects.requireNonNull(captureId);
    this.spaceId = Objects.requireNonNull(spaceId);
    this.floorNum = Objects.requireNonNull(floorNum);
    this.slotNum = Objects.requireNonNull(slotNum);
  }

  public static Animal create(Long captureId, Long spaceId, Integer floorNum, Integer slotNum) {
    return new Animal(captureId, spaceId, floorNum, slotNum);
  }

  public boolean isPlacedIn(Long spaceId) {
    return spaceId != null && Objects.equals(this.spaceId, spaceId);
  }

  public boolean isAt(Long spaceId, Integer floorNum, Integer slotNum) {
    return Objects.equals(this.spaceId, spaceId)
        && Objects.equals(this.floorNum, floorNum)
        && Objects.equals(this.slotNum, slotNum);
  }

  public void moveTo(Long spaceId, Integer floorNum, Integer slotNum) {
    this.spaceId = spaceId;
    this.floorNum = floorNum;
    this.slotNum = slotNum;
  }
}
