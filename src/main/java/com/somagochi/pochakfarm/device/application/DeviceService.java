package com.somagochi.pochakfarm.device.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.device.infrastructure.persistence.AnonymousDeviceRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

  private final AnonymousDeviceRepository deviceRepository;

  public DeviceService(AnonymousDeviceRepository deviceRepository) {
    this.deviceRepository = deviceRepository;
  }

  public AnonymousDevice findByToken(String deviceToken) {
    if (deviceToken == null) {
      throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
    }
    return deviceRepository
        .findByDeviceToken(deviceToken)
        .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
  }
}
