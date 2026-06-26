package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.device.application.DeviceService;
import com.somagochi.pochakfarm.device.domain.AnonymousDevice;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistrationStatus;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreRegistrationRegisterService {

  private static final Pattern PHONE_PATTERN = Pattern.compile("^010\\d{8}$");

  private final DeviceService deviceService;
  private final PreRegistrationRepository preRegistrationRepository;

  public PreRegistrationRegisterService(
      DeviceService deviceService, PreRegistrationRepository preRegistrationRepository) {
    this.deviceService = deviceService;
    this.preRegistrationRepository = preRegistrationRepository;
  }

  @Transactional
  public PreRegistrationResponse register(String deviceToken, PreRegistrationRequest request) {
    AnonymousDevice device = deviceService.findByToken(deviceToken);
    String phoneNumber = normalizePhoneNumber(request.phoneNumber());
    validateRequiredConsent(request);
    validatePhoneNotRegistered(phoneNumber);
    return reactivateOrCreate(
        device.getId(), phoneNumber, Boolean.TRUE.equals(request.marketingConsent()));
  }

  private PreRegistrationResponse reactivateOrCreate(
      Long deviceId, String phoneNumber, boolean marketingConsent) {
    PreRegistration existing = preRegistrationRepository.findByDeviceId(deviceId).orElse(null);
    if (existing != null) {
      // 취소된 예약은 다시 활성화하고, 이미 등록된 기기는 거부한다.
      if (existing.isRegistered()) {
        throw new BusinessException(ErrorCode.DEVICE_ALREADY_REGISTERED);
      }
      existing.reactivate(phoneNumber, true, marketingConsent);
      return PreRegistrationResponse.from(existing);
    }
    return create(deviceId, phoneNumber, marketingConsent);
  }

  private PreRegistrationResponse create(
      Long deviceId, String phoneNumber, boolean marketingConsent) {
    try {
      PreRegistration saved =
          preRegistrationRepository.save(
              PreRegistration.create(deviceId, phoneNumber, true, marketingConsent));
      return PreRegistrationResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      // 동시 요청으로 같은 device 가 먼저 저장된 경우 (uk_pre_registrations_device_id)
      throw new BusinessException(ErrorCode.DEVICE_ALREADY_REGISTERED);
    }
  }

  private void validateRequiredConsent(PreRegistrationRequest request) {
    if (!Boolean.TRUE.equals(request.requiredConsent())) {
      throw new BusinessException(ErrorCode.REQUIRED_CONSENT_REQUIRED);
    }
  }

  private void validatePhoneNotRegistered(String phoneNumber) {
    if (preRegistrationRepository.existsByPhoneNumberAndStatus(
        phoneNumber, PreRegistrationStatus.REGISTERED)) {
      throw new BusinessException(ErrorCode.PHONE_NUMBER_ALREADY_REGISTERED);
    }
  }

  private String normalizePhoneNumber(String phoneNumber) {
    if (phoneNumber == null) {
      throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
    }
    String digits = phoneNumber.replaceAll("\\D", "");
    if (!PHONE_PATTERN.matcher(digits).matches()) {
      throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
    }
    return digits;
  }
}
