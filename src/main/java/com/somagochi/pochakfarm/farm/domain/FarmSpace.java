package com.somagochi.pochakfarm.farm.domain;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "farm_spaces",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_farm_spaces_user_id_type",
            columnNames = {"user_id", "type"}))
public class FarmSpace extends BaseEntity {

  public static final int TOTAL_FLOOR_COUNT = 4;
  public static final int FLOOR_COUNT_PER_PAGE = 4;
  public static final int TOTAL_PAGE_COUNT = TOTAL_FLOOR_COUNT / FLOOR_COUNT_PER_PAGE;
  public static final int FIRST_PAGE = 0;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, updatable = false)
  private CardType type;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  private FarmSpace(Long userId, CardType type) {
    this.userId = userId;
    this.type = type;
  }

  public static FarmSpace create(Long userId, CardType type) {
    return new FarmSpace(userId, type);
  }
}
