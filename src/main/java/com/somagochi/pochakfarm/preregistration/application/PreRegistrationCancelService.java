package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.device.application.DeviceService;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreRegistrationCancelService {

  private final DeviceService deviceService;
  private final PreRegistrationRepository preRegistrationRepository;

  public PreRegistrationCancelService(
      DeviceService deviceService, PreRegistrationRepository preRegistrationRepository) {
    this.deviceService = deviceService;
    this.preRegistrationRepository = preRegistrationRepository;
  }

  @Transactional
  public PreRegistrationResponse cancel(String deviceToken) {
    AnonymousDevice device = deviceService.findByToken(deviceToken);
    PreRegistration preRegistration =
        preRegistrationRepository
            .findByDeviceId(device.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRE_REGISTRATION_NOT_FOUND));
    if (preRegistration.isRegistered()) {
      preRegistration.cancel();
    }
    return PreRegistrationResponse.from(preRegistration);
  }
}
