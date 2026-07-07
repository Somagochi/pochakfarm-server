package com.somagochi.pochakfarm.preregistration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistrationStatus;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreRegistrationCancelServiceTest {

  private static final String PHONE = "01012345678";

  private final PreRegistrationRepository preRegistrationRepository =
      mock(PreRegistrationRepository.class);
  private final PreRegistrationCancelService preRegistrationCancelService =
      new PreRegistrationCancelService(preRegistrationRepository);

  private PreRegistration preRegistration(boolean registered) {
    PreRegistration preRegistration = mock(PreRegistration.class);
    given(preRegistration.getId()).willReturn(5L);
    given(preRegistration.isRegistered()).willReturn(registered);
    given(preRegistration.getStatus())
        .willReturn(registered ? PreRegistrationStatus.REGISTERED : PreRegistrationStatus.CANCELED);
    return preRegistration;
  }

  @Test
  void cancelsRegistration() {
    PreRegistration registered = preRegistration(true);
    given(preRegistrationRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(registered));

    preRegistrationCancelService.cancel(PHONE);

    verify(registered).cancel();
  }

  @Test
  void isIdempotentWhenAlreadyCanceled() {
    PreRegistration canceled = preRegistration(false);
    given(preRegistrationRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(canceled));

    PreRegistrationResponse response = preRegistrationCancelService.cancel(PHONE);

    assertEquals("CANCELED", response.status());
    verify(canceled, never()).cancel();
  }

  @Test
  void rejectsWhenNoRegistration() {
    given(preRegistrationRepository.findByPhoneNumber(PHONE)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> preRegistrationCancelService.cancel(PHONE));

    assertEquals(ErrorCode.PRE_REGISTRATION_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void rejectsInvalidPhoneNumber() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> preRegistrationCancelService.cancel("0100"));

    assertEquals(ErrorCode.INVALID_PHONE_NUMBER.getCode(), exception.getCode());
  }
}
