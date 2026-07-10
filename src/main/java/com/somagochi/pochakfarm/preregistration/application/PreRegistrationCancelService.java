package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreRegistrationCancelService {

  private static final Pattern PHONE_PATTERN = Pattern.compile("^010\\d{8}$");

  private final PreRegistrationRepository preRegistrationRepository;
  private final PreRegistrationCryptoService preRegistrationCryptoService;

  public PreRegistrationCancelService(
      PreRegistrationRepository preRegistrationRepository,
      PreRegistrationCryptoService preRegistrationCryptoService) {
    this.preRegistrationRepository = preRegistrationRepository;
    this.preRegistrationCryptoService = preRegistrationCryptoService;
  }

  @Transactional
  public PreRegistrationResponse cancel(String phoneNumber) {
    String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
    PreRegistration preRegistration =
        preRegistrationRepository
            .findByPhoneNumberHash(preRegistrationCryptoService.hash(normalizedPhoneNumber))
            .orElseThrow(() -> new BusinessException(ErrorCode.PRE_REGISTRATION_NOT_FOUND));
    if (preRegistration.isRegistered()) {
      preRegistration.cancel();
    }
    return PreRegistrationResponse.from(preRegistration);
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
