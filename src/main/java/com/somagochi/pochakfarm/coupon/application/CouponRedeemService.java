package com.somagochi.pochakfarm.coupon.application;

import com.somagochi.pochakfarm.animal.application.AnimalPlacementService;
import com.somagochi.pochakfarm.capture.application.CaptureGrantService;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.application.CharacterizationReadService;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.coupon.dto.CouponRedeemResponse;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.CouponRepository;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationQueryService;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponRedeemService {

  private static final Tier REWARD_TIER = Tier.S;
  private static final String ANIMAL_IMAGE_UPLOAD_PURPOSE = "coupon-animal";
  private static final String ANIMAL_IMAGE_CONTENT_TYPE = "image/png";

  private final CouponRepository couponRepository;
  private final PreRegistrationCouponRecipientRepository recipientRepository;
  private final UserQueryService userQueryService;
  private final PreRegistrationQueryService preRegistrationQueryService;
  private final CharacterizationReadService characterizationReadService;
  private final CaptureGrantService captureGrantService;
  private final AnimalPlacementService animalPlacementService;
  private final FileStorage fileStorage;
  private final ImageUploadService imageUploadService;

  @Transactional
  public CouponRedeemResponse redeem(Long userId, String couponCode) {
    Instant now = Instant.now();
    Coupon coupon = findCoupon(couponCode);
    PreRegistrationCouponRecipient recipient = findRecipient(coupon);
    if (coupon.isUsed() || recipient.isConverted()) {
      throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
    }
    if (recipient.isAssignedTo(userId)) {
      return toResponse(userId, captureGrantService.getById(recipient.getCaptureId()));
    }
    validateAssignable(recipient, coupon, userId, now);

    Characterization characterization = findCharacterization(recipient);
    animalPlacementService.validateHasEmptySlot(userId, characterization.getCardType());
    Capture capture = captureGrantService.grant(userId, characterization, REWARD_TIER, now);
    recipient.assign(userId, capture.getId());
    return toResponse(userId, capture);
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

  private void validateAssignable(
      PreRegistrationCouponRecipient recipient, Coupon coupon, Long userId, Instant now) {
    if (recipient.isAssigned()) {
      throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
    }
    if (coupon.isExpired(now)) {
      throw new BusinessException(ErrorCode.COUPON_EXPIRED);
    }
    userQueryService.getForUpdate(userId);
    if (recipientRepository.existsByUserId(userId)) {
      throw new BusinessException(ErrorCode.COUPON_ALREADY_REDEEMED);
    }
  }

  private Characterization findCharacterization(PreRegistrationCouponRecipient recipient) {
    PreRegistration preRegistration =
        preRegistrationQueryService.getById(recipient.getPreRegistrationId());
    Characterization characterization =
        characterizationReadService.getById(preRegistration.getCharacterizationId());
    if (characterization.getResultImageKey() == null) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_NOT_FOUND);
    }
    return characterization;
  }

  private CouponRedeemResponse toResponse(Long userId, Capture capture) {
    return CouponRedeemResponse.of(
        capture,
        fileStorage.buildUrl(capture.getCardImage()),
        imageUploadService.createPresign(
            userId, ANIMAL_IMAGE_UPLOAD_PURPOSE, ANIMAL_IMAGE_CONTENT_TYPE));
  }
}
