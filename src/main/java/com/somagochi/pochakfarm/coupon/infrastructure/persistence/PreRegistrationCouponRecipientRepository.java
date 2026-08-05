package com.somagochi.pochakfarm.coupon.infrastructure.persistence;

import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreRegistrationCouponRecipientRepository
    extends JpaRepository<PreRegistrationCouponRecipient, Long> {

  Optional<PreRegistrationCouponRecipient> findByCouponId(Long couponId);

  List<PreRegistrationCouponRecipient> findByPreRegistrationIdIn(
      Collection<Long> preRegistrationIds);

  boolean existsByUserId(Long userId);

  boolean existsByUserIdAndConvertedAtIsNotNull(Long userId);

  boolean existsByPreRegistrationId(Long preRegistrationId);
}
