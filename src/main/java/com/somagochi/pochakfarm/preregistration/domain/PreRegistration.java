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

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PreRegistrationStatus status;

  private PreRegistration(
      String phoneNumberEncrypted, String phoneNumberHash, boolean requiredConsent) {
    this.phoneNumberEncrypted = phoneNumberEncrypted;
    this.phoneNumberHash = phoneNumberHash;
    this.requiredConsent = requiredConsent;
    this.status = PreRegistrationStatus.REGISTERED;
  }

  public static PreRegistration create(
      String phoneNumberEncrypted, String phoneNumberHash, boolean requiredConsent) {
    return new PreRegistration(phoneNumberEncrypted, phoneNumberHash, requiredConsent);
  }

  public boolean isRegistered() {
    return status == PreRegistrationStatus.REGISTERED;
  }

  public void cancel() {
    this.status = PreRegistrationStatus.CANCELED;
  }

  public void reactivate(
      String phoneNumberEncrypted, String phoneNumberHash, boolean requiredConsent) {
    this.phoneNumberEncrypted = phoneNumberEncrypted;
    this.phoneNumberHash = phoneNumberHash;
    this.requiredConsent = requiredConsent;
    this.status = PreRegistrationStatus.REGISTERED;
  }
}
