package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.notification.BulkSmsNotification;
import com.somagochi.pochakfarm.common.notification.NotificationResult;
import com.somagochi.pochakfarm.common.notification.NotificationService;
import com.somagochi.pochakfarm.common.notification.SmsNotification;
import com.somagochi.pochakfarm.coupon.application.CouponQueryService;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistrationStatus;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsResult;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PreRegistrationCouponSmsService {

  private static final String COUPON_CODE_PLACEHOLDER = "{couponCode}";

  private final PreRegistrationRepository preRegistrationRepository;
  private final PreRegistrationCryptoService cryptoService;
  private final CouponQueryService couponQueryService;
  private final NotificationService notificationService;
  private final TransactionTemplate transactionTemplate;

  public PreRegistrationCouponSmsResult send(boolean dryRun, String messageTemplate) {
    validateMessageTemplate(messageTemplate);
    List<PreRegistration> targets =
        preRegistrationRepository.findAllByStatusAndMessageSentAtIsNull(
            PreRegistrationStatus.REGISTERED);
    Map<Long, String> couponCodes =
        couponQueryService.findCouponCodesByPreRegistrationIds(
            targets.stream().map(PreRegistration::getId).toList());
    List<PreRegistration> sendables =
        targets.stream().filter(target -> couponCodes.containsKey(target.getId())).toList();
    int couponNotIssuedCount = targets.size() - sendables.size();

    if (dryRun || sendables.isEmpty()) {
      return new PreRegistrationCouponSmsResult(targets.size(), 0, 0, couponNotIssuedCount, dryRun);
    }

    Map<Long, String> phoneNumbers =
        sendables.stream()
            .collect(
                Collectors.toMap(
                    PreRegistration::getId,
                    target -> cryptoService.decrypt(target.getPhoneNumberEncrypted())));
    List<SmsNotification> messages =
        sendables.stream()
            .map(
                target ->
                    new SmsNotification(
                        phoneNumbers.get(target.getId()),
                        messageTemplate.replace(
                            COUPON_CODE_PLACEHOLDER, couponCodes.get(target.getId()))))
            .toList();

    NotificationResult result = notificationService.notify(new BulkSmsNotification(messages));
    Set<String> failedPhoneNumbers = Set.copyOf(result.failedRecipients());
    List<Long> sentIds =
        sendables.stream()
            .filter(target -> !failedPhoneNumbers.contains(phoneNumbers.get(target.getId())))
            .map(PreRegistration::getId)
            .toList();

    transactionTemplate.executeWithoutResult(
        status -> {
          Instant sentAt = Instant.now();
          preRegistrationRepository
              .findAllById(sentIds)
              .forEach(preRegistration -> preRegistration.markCouponSmsSent(sentAt));
        });

    if (!failedPhoneNumbers.isEmpty()) {
      log.warn(
          "pre_registration_coupon_sms_partial_failure sent={} failed={}",
          sentIds.size(),
          failedPhoneNumbers.size());
    }
    return new PreRegistrationCouponSmsResult(
        targets.size(), sentIds.size(), failedPhoneNumbers.size(), couponNotIssuedCount, false);
  }

  private void validateMessageTemplate(String messageTemplate) {
    if (messageTemplate == null
        || messageTemplate.isBlank()
        || !messageTemplate.contains(COUPON_CODE_PLACEHOLDER)) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
  }
}
