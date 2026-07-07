package com.somagochi.pochakfarm.preregistration.dto;

import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;

public record PreRegistrationResponse(Long preRegistrationId, String status) {

  public static PreRegistrationResponse from(PreRegistration preRegistration) {
    return new PreRegistrationResponse(preRegistration.getId(), preRegistration.getStatus().name());
  }
}
