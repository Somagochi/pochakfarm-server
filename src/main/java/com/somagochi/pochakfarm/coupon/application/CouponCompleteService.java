package com.somagochi.pochakfarm.coupon.application;

import com.somagochi.pochakfarm.capture.application.CaptureGrantService;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.coupon.dto.CouponCompleteResponse;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.CouponRepository;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import com.somagochi.pochakfarm.user.domain.User;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponCompleteService {

  private static final long REWARD_COINS = 3000L;
  private static final String ANIMAL_IMAGE_CONTENT_TYPE = "image/png";

  private final CouponRepository couponRepository;
  private final PreRegistrationCouponRecipientRepository recipientRepository;
  private final UserQueryService userQueryService;
  private final CaptureGrantService captureGrantService;
  private final ImageUploadService imageUploadService;

  @Transactional
  public CouponCompleteResponse complete(Long userId, String couponCode, String animalImageKey) {
    Coupon coupon = findCoupon(couponCode);
    PreRegistrationCouponRecipient recipient = findRecipient(coupon);
    validateCompletable(coupon, recipient, userId);

    imageUploadService.validateUploadedObject(userId, animalImageKey, ANIMAL_IMAGE_CONTENT_TYPE);
    captureGrantService.completeGrant(userId, recipient.getCaptureId(), animalImageKey);

    User user = userQueryService.getForUpdate(userId);
    user.addCoins(REWARD_COINS);
    coupon.use();
    recipient.convert(Instant.now());
    return new CouponCompleteResponse(REWARD_COINS, user.getCoins());
  }

  private Coupon findCoupon(String couponCode) {
    return couponRepository
        .findByCouponCodeForUpdate(couponCode)
        .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
  }

  private PreRegistrationCouponRecipient findRecipient(Coupon coupon) {
    return recipientRepository
        .findByCouponId(coupon.getId())
        .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
  }

  private void validateCompletable(
      Coupon coupon, PreRegistrationCouponRecipient recipient, Long userId) {
    if (!recipient.isAssigned()) {
      throw new BusinessException(ErrorCode.COUPON_REDEEM_NOT_STARTED);
    }
    if (!recipient.isAssignedTo(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_COUPON_ACCESS);
    }
    if (coupon.isUsed() || recipient.isConverted()) {
      throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
    }
  }
}
