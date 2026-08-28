package com.somagochi.pochakfarm.common.entity;

import java.util.Objects;

public record EntityChangedEvent(BaseEntity entity, EntityChangeType changeType) {

  public EntityChangedEvent {
    Objects.requireNonNull(entity);
    Objects.requireNonNull(changeType);
  }
}
