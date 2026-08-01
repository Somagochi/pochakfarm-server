package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreRegistrationQueryService {

  private final PreRegistrationRepository preRegistrationRepository;

  public PreRegistration getById(Long preRegistrationId) {
    return preRegistrationRepository
        .findById(preRegistrationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRE_REGISTRATION_NOT_FOUND));
  }
}
