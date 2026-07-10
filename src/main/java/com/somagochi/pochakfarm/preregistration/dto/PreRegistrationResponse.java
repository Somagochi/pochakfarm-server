package com.somagochi.pochakfarm.preregistration.dto;

import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import io.swagger.v3.oas.annotations.media.Schema;

public record PreRegistrationResponse(
    @Schema(description = "사전예약 ID", example = "1") Long preRegistrationId,
    @Schema(
            description = "사전예약 상태",
            example = "REGISTERED",
            allowableValues = {"REGISTERED", "CANCELED"})
        String status) {

  public static PreRegistrationResponse from(PreRegistration preRegistration) {
    return new PreRegistrationResponse(preRegistration.getId(), preRegistration.getStatus().name());
  }
}
