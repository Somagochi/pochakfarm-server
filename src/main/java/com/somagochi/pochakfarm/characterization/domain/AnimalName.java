package com.somagochi.pochakfarm.characterization.domain;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalName {

  private static final int MAX_LENGTH = 6;

  @Column(name = "animal_name", nullable = false)
  private String value;

  private AnimalName(String animalName) {
    this.value = normalize(animalName);
  }

  public static AnimalName from(String animalName) {
    return new AnimalName(animalName);
  }

  public String value() {
    return value;
  }

  private String normalize(String animalName) {
    if (animalName == null || animalName.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_ANIMAL_NAME);
    }
    String normalized = animalName.trim();
    if (normalized.length() > MAX_LENGTH) {
      throw new BusinessException(ErrorCode.INVALID_ANIMAL_NAME);
    }
    return normalized;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AnimalName that)) {
      return false;
    }
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
