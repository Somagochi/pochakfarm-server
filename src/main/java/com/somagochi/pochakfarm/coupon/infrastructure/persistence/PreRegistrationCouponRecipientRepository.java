package com.somagochi.pochakfarm.coupon.infrastructure.persistence;

import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreRegistrationCouponRecipientRepository
    extends JpaRepository<PreRegistrationCouponRecipient, Long> {

  Optional<PreRegistrationCouponRecipient> findByCouponId(Long couponId);

  boolean existsByUserId(Long userId);

  boolean existsByPreRegistrationId(Long preRegistrationId);
}
