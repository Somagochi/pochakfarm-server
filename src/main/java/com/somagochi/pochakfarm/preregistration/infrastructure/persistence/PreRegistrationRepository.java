package com.somagochi.pochakfarm.preregistration.infrastructure.persistence;

import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistrationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreRegistrationRepository extends JpaRepository<PreRegistration, Long> {

  Optional<PreRegistration> findByPhoneNumberHash(String phoneNumberHash);

  List<PreRegistration> findAllByStatus(PreRegistrationStatus status);

  List<PreRegistration> findAllByStatusAndCouponSmsSentAtIsNull(PreRegistrationStatus status);
}
