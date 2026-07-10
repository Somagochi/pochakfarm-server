package com.somagochi.pochakfarm.device.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "anonymous_devices",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_anonymous_devices_device_token",
            columnNames = {"device_token"}))
public class AnonymousDevice extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_token", nullable = false, updatable = false)
  private String deviceToken;

  @Column(name = "ai_generated_used")
  private Boolean aiGeneratedUsed = false;

  @Column(name = "ai_generated_at")
  private Instant aiGeneratedAt;

  private AnonymousDevice(String deviceToken) {
    this.deviceToken = deviceToken;
  }

  public static AnonymousDevice issue(String deviceToken) {
    return new AnonymousDevice(deviceToken);
  }
}
