package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
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
  private final PreRegistrationCryptoService preRegistrationCryptoService;
  private final CharacterizationRepository characterizationRepository;

  public PreRegistrationRegisterService(
      PreRegistrationRepository preRegistrationRepository,
      PreRegistrationCryptoService preRegistrationCryptoService,
      CharacterizationRepository characterizationRepository) {
    this.preRegistrationRepository = preRegistrationRepository;
    this.preRegistrationCryptoService = preRegistrationCryptoService;
    this.characterizationRepository = characterizationRepository;
  }

  @Transactional
  public PreRegistrationResponse register(PreRegistrationRequest request) {
    String phoneNumber = normalizePhoneNumber(request.phoneNumber());
    validateRequiredConsent(request);
    Long characterizationId = validateCharacterizationId(request.characterizationId());
    return reactivateOrCreate(phoneNumber, characterizationId);
  }

  private PreRegistrationResponse reactivateOrCreate(String phoneNumber, Long characterizationId) {
    String phoneNumberHash = preRegistrationCryptoService.hash(phoneNumber);
    PreRegistration existing =
        preRegistrationRepository.findByPhoneNumberHash(phoneNumberHash).orElse(null);
    if (existing != null) {
      if (existing.isRegistered()) {
        throw new BusinessException(ErrorCode.PHONE_NUMBER_ALREADY_REGISTERED);
      }
      existing.reactivate(
          preRegistrationCryptoService.encrypt(phoneNumber),
          phoneNumberHash,
          true,
          characterizationId);
      return PreRegistrationResponse.from(existing);
    }
    return create(phoneNumber, phoneNumberHash, characterizationId);
  }

  private PreRegistrationResponse create(
      String phoneNumber, String phoneNumberHash, Long characterizationId) {
    try {
      PreRegistration saved =
          preRegistrationRepository.save(
              PreRegistration.create(
                  preRegistrationCryptoService.encrypt(phoneNumber),
                  phoneNumberHash,
                  true,
                  characterizationId));
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

  private Long validateCharacterizationId(Long characterizationId) {
    if (characterizationId == null || characterizationId <= 0) {
      throw new BusinessException(ErrorCode.INVALID_CHARACTERIZATION_ID);
    }
    if (!characterizationRepository.existsById(characterizationId)) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_NOT_FOUND);
    }
    return characterizationId;
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
