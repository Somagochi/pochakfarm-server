package com.somagochi.pochakfarm.preregistration.domain;

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
    name = "pre_registrations",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_pre_registrations_phone_number",
            columnNames = {"phone_number"}))
public class PreRegistration extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "phone_number", nullable = false)
  private String phoneNumber;

  @Column(name = "required_consent", nullable = false)
  private boolean requiredConsent;

  @Column(name = "marketing_consent", nullable = false)
  private boolean marketingConsent;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PreRegistrationStatus status;

  private PreRegistration(String phoneNumber, boolean requiredConsent) {
    this.phoneNumber = phoneNumber;
    this.requiredConsent = requiredConsent;
    this.marketingConsent = false;
    this.status = PreRegistrationStatus.REGISTERED;
  }

  public static PreRegistration create(String phoneNumber, boolean requiredConsent) {
    return new PreRegistration(phoneNumber, requiredConsent);
  }

  public boolean isRegistered() {
    return status == PreRegistrationStatus.REGISTERED;
  }

  public void cancel() {
    this.status = PreRegistrationStatus.CANCELED;
  }

  public void reactivate(String phoneNumber, boolean requiredConsent) {
    this.phoneNumber = phoneNumber;
    this.requiredConsent = requiredConsent;
    this.marketingConsent = false;
    this.status = PreRegistrationStatus.REGISTERED;
  }
}
