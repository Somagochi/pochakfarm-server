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
    name = "farm_floors",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_farm_floors_space_id_sequence",
            columnNames = {"space_id", "sequence"}))
@SQLRestriction("deleted_at is null")
public class FarmFloor extends BaseEntity {

  public static final int SLOT_COUNT_PER_FLOOR = 4;
  public static final int FIRST_SEQUENCE = 1;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sequence", nullable = false, updatable = false)
  private Integer sequence;

  @Column(name = "space_id", nullable = false, updatable = false)
  private Long spaceId;

  private FarmFloor(Long spaceId, Integer sequence) {
    this.spaceId = spaceId;
    this.sequence = sequence;
  }

  public static FarmFloor create(Long spaceId, Integer sequence) {
    return new FarmFloor(spaceId, sequence);
  }
}
