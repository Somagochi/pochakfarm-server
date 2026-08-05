package com.somagochi.pochakfarm.preregistration.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "pre_registrations",
    indexes =
        @Index(name = "idx_pre_registrations_phone_number_hash", columnList = "phone_number_hash"),
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_pre_registrations_phone_number_hash",
            columnNames = {"phone_number_hash"}))
public class PreRegistration extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "phone_number_encrypted", nullable = false, length = 512)
  private String phoneNumberEncrypted;

  @Column(name = "phone_number_hash", nullable = false, length = 128)
  private String phoneNumberHash;

  @Column(name = "required_consent", nullable = false)
  private boolean requiredConsent;

  @Column(name = "characterization_id", nullable = false)
  private Long characterizationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PreRegistrationStatus status;

  @Column(name = "message_sent_at")
  private Instant messageSentAt;

  private PreRegistration(
      String phoneNumberEncrypted,
      String phoneNumberHash,
      boolean requiredConsent,
      Long characterizationId) {
    this.phoneNumberEncrypted = phoneNumberEncrypted;
    this.phoneNumberHash = phoneNumberHash;
    this.requiredConsent = requiredConsent;
    this.characterizationId = characterizationId;
    this.status = PreRegistrationStatus.REGISTERED;
  }

  public static PreRegistration create(
      String phoneNumberEncrypted,
      String phoneNumberHash,
      boolean requiredConsent,
      Long characterizationId) {
    return new PreRegistration(
        phoneNumberEncrypted, phoneNumberHash, requiredConsent, characterizationId);
  }

  public boolean isRegistered() {
    return status == PreRegistrationStatus.REGISTERED;
  }

  public void cancel() {
    this.status = PreRegistrationStatus.CANCELED;
  }

  public void markCouponSmsSent(Instant sentAt) {
    this.messageSentAt = Objects.requireNonNull(sentAt);
  }

  public void reactivate(
      String phoneNumberEncrypted,
      String phoneNumberHash,
      boolean requiredConsent,
      Long characterizationId) {
    this.phoneNumberEncrypted = phoneNumberEncrypted;
    this.phoneNumberHash = phoneNumberHash;
    this.requiredConsent = requiredConsent;
    this.characterizationId = characterizationId;
    this.status = PreRegistrationStatus.REGISTERED;
  }
}
