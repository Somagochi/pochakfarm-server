package com.somagochi.pochakfarm.common.entity;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public final class EntityChangeEventPublisher {

  // JPA 콜백을 우회하는 DB 직접/벌크 변경까지 보장하려면 CDC 또는 Outbox가 필요하다.
  private static volatile ApplicationEventPublisher applicationEventPublisher;

  private EntityChangeEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    EntityChangeEventPublisher.applicationEventPublisher = applicationEventPublisher;
  }

  static void publish(BaseEntity entity, EntityChangeType changeType) {
    ApplicationEventPublisher publisher = applicationEventPublisher;
    if (publisher != null) {
      publisher.publishEvent(new EntityChangedEvent(entity, changeType));
    }
  }
}
