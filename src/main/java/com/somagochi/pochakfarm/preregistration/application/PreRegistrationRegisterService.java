package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
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

  private final PreRegistrationRepository preRegistrationRepository;

  public PreRegistrationRegisterService(PreRegistrationRepository preRegistrationRepository) {
    this.preRegistrationRepository = preRegistrationRepository;
  }

  @Transactional
  public PreRegistrationResponse register(PreRegistrationRequest request) {
    String phoneNumber = normalizePhoneNumber(request.phoneNumber());
    validateRequiredConsent(request);
    return reactivateOrCreate(phoneNumber, Boolean.TRUE.equals(request.marketingConsent()));
  }

  private PreRegistrationResponse reactivateOrCreate(String phoneNumber, boolean marketingConsent) {
    PreRegistration existing =
        preRegistrationRepository.findByPhoneNumber(phoneNumber).orElse(null);
    if (existing != null) {
      if (existing.isRegistered()) {
        throw new BusinessException(ErrorCode.PHONE_NUMBER_ALREADY_REGISTERED);
      }
      existing.reactivate(phoneNumber, true, marketingConsent);
      return PreRegistrationResponse.from(existing);
    }
    return create(phoneNumber, marketingConsent);
  }

  private PreRegistrationResponse create(String phoneNumber, boolean marketingConsent) {
    try {
      PreRegistration saved =
          preRegistrationRepository.save(
              PreRegistration.create(phoneNumber, true, marketingConsent));
      return PreRegistrationResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      // 동시 요청으로 같은 phone number 가 먼저 저장된 경우
      throw new BusinessException(ErrorCode.PHONE_NUMBER_ALREADY_REGISTERED);
    }
  }

  private void validateRequiredConsent(PreRegistrationRequest request) {
    if (!Boolean.TRUE.equals(request.requiredConsent())) {
      throw new BusinessException(ErrorCode.REQUIRED_CONSENT_REQUIRED);
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
