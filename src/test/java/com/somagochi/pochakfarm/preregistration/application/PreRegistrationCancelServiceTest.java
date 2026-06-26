package com.somagochi.pochakfarm.preregistration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.device.application.DeviceService;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistrationStatus;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreRegistrationCancelServiceTest {

  private final DeviceService deviceService = mock(DeviceService.class);
  private final PreRegistrationRepository preRegistrationRepository =
      mock(PreRegistrationRepository.class);
  private final PreRegistrationCancelService preRegistrationCancelService =
      new PreRegistrationCancelService(deviceService, preRegistrationRepository);

  private void givenDevice() {
    AnonymousDevice device = mock(AnonymousDevice.class);
    given(device.getId()).willReturn(1L);
    given(deviceService.findByToken("dev_abc")).willReturn(device);
  }

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
    givenDevice();
    PreRegistration registered = preRegistration(true);
    given(preRegistrationRepository.findByDeviceId(1L)).willReturn(Optional.of(registered));

    preRegistrationCancelService.cancel("dev_abc");

    verify(registered).cancel();
  }

  @Test
  void isIdempotentWhenAlreadyCanceled() {
    givenDevice();
    PreRegistration canceled = preRegistration(false);
    given(preRegistrationRepository.findByDeviceId(1L)).willReturn(Optional.of(canceled));

    PreRegistrationResponse response = preRegistrationCancelService.cancel("dev_abc");

    assertEquals("CANCELED", response.status());
    verify(canceled, never()).cancel();
  }

  @Test
  void rejectsWhenNoRegistration() {
    givenDevice();
    given(preRegistrationRepository.findByDeviceId(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> preRegistrationCancelService.cancel("dev_abc"));

    assertEquals(ErrorCode.PRE_REGISTRATION_NOT_FOUND.getCode(), exception.getCode());
  }
}
