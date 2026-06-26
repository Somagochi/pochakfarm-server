package com.somagochi.pochakfarm.preregistration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreRegistrationRegisterServiceTest {

  private static final String PHONE = "01012345678";

  private final DeviceService deviceService = mock(DeviceService.class);
  private final PreRegistrationRepository preRegistrationRepository =
      mock(PreRegistrationRepository.class);
  private final PreRegistrationRegisterService preRegistrationService =
      new PreRegistrationRegisterService(deviceService, preRegistrationRepository);

  private PreRegistrationRequest request(String phone, Boolean required, Boolean marketing) {
    return new PreRegistrationRequest(phone, required, marketing);
  }

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
  void registersNewPreRegistration() {
    givenDevice();
    given(
            preRegistrationRepository.existsByPhoneNumberAndStatus(
                PHONE, PreRegistrationStatus.REGISTERED))
        .willReturn(false);
    given(preRegistrationRepository.findByDeviceId(1L)).willReturn(Optional.empty());
    PreRegistration saved = preRegistration(true);
    given(preRegistrationRepository.save(any())).willReturn(saved);

    PreRegistrationResponse response =
        preRegistrationService.register("dev_abc", request(PHONE, true, false));

    assertEquals(5L, response.preRegistrationId());
    assertEquals("REGISTERED", response.status());
  }

  @Test
  void reactivatesCanceledRegistration() {
    givenDevice();
    given(
            preRegistrationRepository.existsByPhoneNumberAndStatus(
                PHONE, PreRegistrationStatus.REGISTERED))
        .willReturn(false);
    PreRegistration canceled = preRegistration(false);
    given(preRegistrationRepository.findByDeviceId(1L)).willReturn(Optional.of(canceled));

    preRegistrationService.register("dev_abc", request(PHONE, true, true));

    verify(canceled).reactivate(PHONE, true, true);
    verify(preRegistrationRepository, never()).save(any());
  }

  @Test
  void rejectsInvalidPhoneNumber() {
    givenDevice();

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> preRegistrationService.register("dev_abc", request("0100", true, false)));

    assertEquals(ErrorCode.INVALID_PHONE_NUMBER.getCode(), exception.getCode());
  }

  @Test
  void rejectsWhenRequiredConsentMissing() {
    givenDevice();

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> preRegistrationService.register("dev_abc", request(PHONE, false, false)));

    assertEquals(ErrorCode.REQUIRED_CONSENT_REQUIRED.getCode(), exception.getCode());
  }

  @Test
  void rejectsAlreadyRegisteredDevice() {
    givenDevice();
    given(
            preRegistrationRepository.existsByPhoneNumberAndStatus(
                PHONE, PreRegistrationStatus.REGISTERED))
        .willReturn(false);
    PreRegistration registered = preRegistration(true);
    given(preRegistrationRepository.findByDeviceId(1L)).willReturn(Optional.of(registered));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> preRegistrationService.register("dev_abc", request(PHONE, true, false)));

    assertEquals(ErrorCode.DEVICE_ALREADY_REGISTERED.getCode(), exception.getCode());
  }

  @Test
  void rejectsDuplicatePhoneNumber() {
    givenDevice();
    given(
            preRegistrationRepository.existsByPhoneNumberAndStatus(
                PHONE, PreRegistrationStatus.REGISTERED))
        .willReturn(true);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> preRegistrationService.register("dev_abc", request(PHONE, true, false)));

    assertEquals(ErrorCode.PHONE_NUMBER_ALREADY_REGISTERED.getCode(), exception.getCode());
  }
}
