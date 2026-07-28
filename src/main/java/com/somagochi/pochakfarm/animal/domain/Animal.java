package com.somagochi.pochakfarm.animal.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

  public void moveTo(int floorNum, int slotNum) {
    this.floorNum = floorNum;
    this.slotNum = slotNum;
  }
}
