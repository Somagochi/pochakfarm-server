package com.somagochi.pochakfarm.develop.application;

import com.somagochi.pochakfarm.develop.dto.DevelopDeviceTokenResponse;
import com.somagochi.pochakfarm.device.application.DeviceTokenGenerator;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.device.infrastructure.persistence.AnonymousDeviceRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 카드 생성 없이 사전예약 플로우를 테스트할 수 있도록, 익명 디바이스를 만들고 deviceToken 을 발급한다. 운영 코드(device 패키지)의 공개 API 만 사용한다.
 */
@Service
@Profile({"local", "dev"})
public class DevelopDeviceService {

  private final DeviceTokenGenerator deviceTokenGenerator;
  private final AnonymousDeviceRepository deviceRepository;

  public DevelopDeviceService(
      DeviceTokenGenerator deviceTokenGenerator, AnonymousDeviceRepository deviceRepository) {
    this.deviceTokenGenerator = deviceTokenGenerator;
    this.deviceRepository = deviceRepository;
  }

  public DevelopDeviceTokenResponse issueDeviceToken() {
    String deviceToken = deviceTokenGenerator.generate();
    deviceRepository.save(AnonymousDevice.issue(deviceToken));
    return new DevelopDeviceTokenResponse(deviceToken);
  }
}
