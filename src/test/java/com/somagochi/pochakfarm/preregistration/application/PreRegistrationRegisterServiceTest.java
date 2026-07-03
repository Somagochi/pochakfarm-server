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
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistrationStatus;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PreRegistrationRegisterServiceTest {

  private static final String PHONE = "01012345678";
  private static final String PHONE_HASH = "phone-hash";
  private static final String PHONE_ENCRYPTED = "phone-encrypted";

  private final PreRegistrationRepository preRegistrationRepository =
      mock(PreRegistrationRepository.class);
  private final PreRegistrationCryptoService preRegistrationCryptoService =
      mock(PreRegistrationCryptoService.class);
  private final PreRegistrationRegisterService preRegistrationService =
      new PreRegistrationRegisterService(preRegistrationRepository, preRegistrationCryptoService);

  @BeforeEach
  void setUp() {
    given(preRegistrationCryptoService.hash(PHONE)).willReturn(PHONE_HASH);
    given(preRegistrationCryptoService.encrypt(PHONE)).willReturn(PHONE_ENCRYPTED);
  }

  private PreRegistrationRequest request(String phone, Boolean required) {
    return new PreRegistrationRequest(phone, required);
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
  void preRegistrationKeepsOnlyCurrentPersistenceFields() {
    Set<String> fieldNames =
        Arrays.stream(PreRegistration.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("id", "phoneNumberEncrypted", "phoneNumberHash", "requiredConsent", "status"),
        fieldNames);
  }

  @Test
  void registersNewPreRegistration() {
    given(preRegistrationRepository.findByPhoneNumberHash(PHONE_HASH)).willReturn(Optional.empty());
    PreRegistration saved = preRegistration(true);
    given(preRegistrationRepository.save(any())).willReturn(saved);

    PreRegistrationResponse response = preRegistrationService.register(request(PHONE, true));

    assertEquals(5L, response.preRegistrationId());
    assertEquals("REGISTERED", response.status());
  }

  @Test
  void storesEncryptedPhoneNumberAndHashWithoutPlainPhoneNumber() {
    given(preRegistrationRepository.findByPhoneNumberHash(PHONE_HASH)).willReturn(Optional.empty());
    given(preRegistrationRepository.save(any()))
        .willAnswer(invocation -> invocation.getArgument(0));

    preRegistrationService.register(request(PHONE, true));

    ArgumentCaptor<PreRegistration> captor = ArgumentCaptor.forClass(PreRegistration.class);
    verify(preRegistrationRepository).save(captor.capture());
    PreRegistration saved = captor.getValue();

    assertEquals(PHONE_ENCRYPTED, saved.getPhoneNumberEncrypted());
    assertEquals(PHONE_HASH, saved.getPhoneNumberHash());
  }

  @Test
  void reactivatesCanceledRegistrationByPhoneNumber() {
    PreRegistration canceled = preRegistration(false);
    given(preRegistrationRepository.findByPhoneNumberHash(PHONE_HASH))
        .willReturn(Optional.of(canceled));

    preRegistrationService.register(request(PHONE, true));

    verify(canceled).reactivate(PHONE_ENCRYPTED, PHONE_HASH, true);
    verify(preRegistrationRepository, never()).save(any());
  }

  @Test
  void rejectsInvalidPhoneNumber() {
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> preRegistrationService.register(request("0100", true)));

    assertEquals(ErrorCode.INVALID_PHONE_NUMBER.getCode(), exception.getCode());
  }

  @Test
  void rejectsWhenRequiredConsentMissing() {
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> preRegistrationService.register(request(PHONE, false)));

    assertEquals(ErrorCode.REQUIRED_CONSENT_REQUIRED.getCode(), exception.getCode());
  }

  @Test
  void rejectsAlreadyRegisteredPhoneNumber() {
    PreRegistration registered = preRegistration(true);
    given(preRegistrationRepository.findByPhoneNumberHash(PHONE_HASH))
        .willReturn(Optional.of(registered));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> preRegistrationService.register(request(PHONE, true)));

    assertEquals(ErrorCode.PHONE_NUMBER_ALREADY_REGISTERED.getCode(), exception.getCode());
  }

  @Test
  void rejectsConcurrentDuplicatePhoneNumber() {
    given(preRegistrationRepository.findByPhoneNumberHash(PHONE_HASH)).willReturn(Optional.empty());
    given(preRegistrationRepository.save(any()))
        .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> preRegistrationService.register(request(PHONE, true)));

    assertEquals(ErrorCode.PHONE_NUMBER_ALREADY_REGISTERED.getCode(), exception.getCode());
  }
}
