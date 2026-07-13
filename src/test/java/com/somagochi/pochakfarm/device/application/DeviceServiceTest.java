package com.somagochi.pochakfarm.device.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.device.infrastructure.persistence.AnonymousDeviceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeviceServiceTest {

  private final AnonymousDeviceRepository deviceRepository = mock(AnonymousDeviceRepository.class);
  private final DeviceTokenGenerator deviceTokenGenerator = mock(DeviceTokenGenerator.class);
  private final DeviceService deviceService =
      new DeviceService(deviceRepository, deviceTokenGenerator);

  @Test
  void returnsDeviceForToken() {
    AnonymousDevice device = mock(AnonymousDevice.class);
    given(deviceRepository.findByDeviceToken("dev_abc")).willReturn(Optional.of(device));

    assertSame(device, deviceService.findByToken("dev_abc"));
  }

  @Test
  void throwsWhenTokenMissing() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> deviceService.findByToken(null));

    assertEquals(ErrorCode.DEVICE_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void throwsWhenDeviceNotFound() {
    given(deviceRepository.findByDeviceToken("dev_abc")).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> deviceService.findByToken("dev_abc"));

    assertEquals(ErrorCode.DEVICE_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void issuesAnonymousDeviceWhenTokenIsMissing() {
    given(deviceTokenGenerator.generate()).willReturn("dev_new");
    AnonymousDevice saved = AnonymousDevice.issue("dev_new");
    given(deviceRepository.save(argThat(device -> device.getDeviceToken().equals("dev_new"))))
        .willReturn(saved);

    DeviceService.DeviceResolution resolution = deviceService.resolveOrIssue(null);

    assertSame(saved, resolution.device());
    assertEquals(true, resolution.issued());
    verify(deviceRepository).save(argThat(device -> device.getDeviceToken().equals("dev_new")));
  }

  @Test
  void issuesAnonymousDeviceWhenTokenNotFound() {
    given(deviceRepository.findByDeviceToken("dev_missing")).willReturn(Optional.empty());
    given(deviceTokenGenerator.generate()).willReturn("dev_new");
    AnonymousDevice saved = AnonymousDevice.issue("dev_new");
    given(deviceRepository.save(argThat(device -> device.getDeviceToken().equals("dev_new"))))
        .willReturn(saved);

    DeviceService.DeviceResolution resolution = deviceService.resolveOrIssue("dev_missing");

    assertSame(saved, resolution.device());
    assertEquals(true, resolution.issued());
    verify(deviceRepository).save(argThat(device -> device.getDeviceToken().equals("dev_new")));
  }

  @Test
  void resolvesExistingDeviceWhenTokenIsPresent() {
    AnonymousDevice device = mock(AnonymousDevice.class);
    given(deviceRepository.findByDeviceToken("dev_abc")).willReturn(Optional.of(device));

    DeviceService.DeviceResolution resolution = deviceService.resolveOrIssue("dev_abc");

    assertSame(device, resolution.device());
    assertEquals(false, resolution.issued());
  }
}
