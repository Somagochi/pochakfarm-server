package com.somagochi.pochakfarm.coupon.application;

import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.CouponRepository;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponQueryService {

  private final PreRegistrationCouponRecipientRepository recipientRepository;
  private final CouponRepository couponRepository;

  @Transactional(readOnly = true)
  public boolean hasConvertedPreRegistrationCoupon(Long userId) {
    return recipientRepository.existsByUserIdAndConvertedAtIsNotNull(userId);
  }

  @Transactional(readOnly = true)
  public Map<Long, String> findCouponCodesByPreRegistrationIds(
      Collection<Long> preRegistrationIds) {
    if (preRegistrationIds.isEmpty()) {
      return Map.of();
    }
    List<PreRegistrationCouponRecipient> recipients =
        recipientRepository.findByPreRegistrationIdIn(preRegistrationIds);
    Map<Long, Coupon> couponById =
        couponRepository
            .findAllById(
                recipients.stream().map(PreRegistrationCouponRecipient::getCouponId).toList())
            .stream()
            .collect(Collectors.toMap(Coupon::getId, Function.identity()));
    return recipients.stream()
        .filter(recipient -> couponById.containsKey(recipient.getCouponId()))
        .collect(
            Collectors.toMap(
                PreRegistrationCouponRecipient::getPreRegistrationId,
                recipient -> couponById.get(recipient.getCouponId()).getCouponCode()));
  }
}
