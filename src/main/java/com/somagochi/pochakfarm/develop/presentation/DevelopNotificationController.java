package com.somagochi.pochakfarm.develop.presentation;

import com.somagochi.pochakfarm.common.notification.NotificationService;
import com.somagochi.pochakfarm.common.notification.SmsNotification;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.develop.dto.DevelopSmsSendRequest;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationCouponSmsService;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/notifications")
@Profile({"local", "dev"})
public class DevelopNotificationController {

  private final NotificationService notificationService;
  private final PreRegistrationCouponSmsService preRegistrationCouponSmsService;

  @PostMapping("/sms")
  public ApiResponse<Void> sendSms(@RequestBody DevelopSmsSendRequest request) {
    notificationService.notify(new SmsNotification(request.to(), request.text()));
    return ApiResponse.empty();
  }

  @PostMapping("/pre-registration-coupon-sms")
  public ApiResponse<PreRegistrationCouponSmsResult> sendPreRegistrationCouponSms(
      @RequestParam(defaultValue = "true") boolean dryRun) {
    return ApiResponse.success(preRegistrationCouponSmsService.send(dryRun));
  }
}
