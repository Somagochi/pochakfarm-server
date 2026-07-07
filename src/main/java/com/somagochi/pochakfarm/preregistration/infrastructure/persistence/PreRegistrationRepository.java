package com.somagochi.pochakfarm.preregistration.infrastructure.persistence;

import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreRegistrationRepository extends JpaRepository<PreRegistration, Long> {

  Optional<PreRegistration> findByPhoneNumber(String phoneNumber);
}
