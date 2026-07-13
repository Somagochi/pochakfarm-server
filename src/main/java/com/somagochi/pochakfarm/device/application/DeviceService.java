package com.somagochi.pochakfarm.device.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.device.infrastructure.persistence.AnonymousDeviceRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

  private final AnonymousDeviceRepository deviceRepository;
  private final DeviceTokenGenerator deviceTokenGenerator;

  public DeviceService(
      AnonymousDeviceRepository deviceRepository, DeviceTokenGenerator deviceTokenGenerator) {
    this.deviceRepository = deviceRepository;
    this.deviceTokenGenerator = deviceTokenGenerator;
  }

  public AnonymousDevice findByToken(String deviceToken) {
    if (deviceToken == null) {
      throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
    }
    return deviceRepository
        .findByDeviceToken(deviceToken)
        .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
  }

  public DeviceResolution resolveOrIssue(String deviceToken) {
    if (deviceToken != null && !deviceToken.isBlank()) {
      Optional<AnonymousDevice> existing = deviceRepository.findByDeviceToken(deviceToken);
      if (existing.isPresent()) {
        return new DeviceResolution(existing.get(), false);
      }
    }
    AnonymousDevice device =
        deviceRepository.save(AnonymousDevice.issue(deviceTokenGenerator.generate()));
    return new DeviceResolution(device, true);
  }

  public record DeviceResolution(AnonymousDevice device, boolean issued) {}
}
