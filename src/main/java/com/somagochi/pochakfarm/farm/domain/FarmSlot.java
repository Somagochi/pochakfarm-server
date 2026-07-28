package com.somagochi.pochakfarm.farm.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "farm_slots",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_farm_slots_floor_id_sequence",
            columnNames = {"floor_id", "sequence"}))
@SQLRestriction("deleted_at is null")
public class FarmSlot extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sequence", nullable = false, updatable = false)
  private Integer sequence;

  @Column(name = "floor_id", nullable = false, updatable = false)
  private Long floorId;

  private FarmSlot(Long floorId, Integer sequence) {
    this.floorId = floorId;
    this.sequence = sequence;
  }

  public static FarmSlot create(Long floorId, Integer sequence) {
    return new FarmSlot(floorId, sequence);
  }
}
