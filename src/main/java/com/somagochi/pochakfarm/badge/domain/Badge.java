package com.somagochi.pochakfarm.badge.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "badges",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_badges_code",
            columnNames = {"code"}))
@SQLRestriction("deleted_at is null")
public class Badge extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, updatable = false, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "image_key")
  private String imageKey;

  private Badge(String code, String name, String description, String imageKey) {
    this.code = Objects.requireNonNull(code);
    this.name = Objects.requireNonNull(name);
    this.description = description;
    this.imageKey = imageKey;
  }

  public static Badge create(String code, String name, String description, String imageKey) {
    return new Badge(code, name, description, imageKey);
  }

  public void changeImageKey(String imageKey) {
    this.imageKey = imageKey;
  }
}
