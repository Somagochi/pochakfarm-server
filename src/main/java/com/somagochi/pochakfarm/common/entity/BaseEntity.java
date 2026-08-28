package com.somagochi.pochakfarm.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void delete(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  @PostPersist
  private void publishCreatedEvent() {
    EntityChangeEventPublisher.publish(this, EntityChangeType.CREATED);
  }

  @PostUpdate
  private void publishUpdatedEvent() {
    EntityChangeEventPublisher.publish(this, EntityChangeType.UPDATED);
  }

  @PostRemove
  private void publishDeletedEvent() {
    EntityChangeEventPublisher.publish(this, EntityChangeType.DELETED);
  }
}
