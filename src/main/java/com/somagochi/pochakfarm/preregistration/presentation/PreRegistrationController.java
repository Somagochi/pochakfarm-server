package com.somagochi.pochakfarm.preregistration.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.AdminApiKeyValidator;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationCancelService;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationCouponSmsService;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationRegisterService;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsResult;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pre-registrations")
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.pre-registration",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PreRegistrationController implements PreRegistrationApiSpec {

  private final PreRegistrationRegisterService preRegistrationRegisterService;
  private final PreRegistrationCancelService preRegistrationCancelService;
  private final PreRegistrationCouponSmsService preRegistrationCouponSmsService;
  private final AdminApiKeyValidator adminApiKeyValidator;

  @Override
  @Deprecated
  public ApiResponse<PreRegistrationResponse> register(
      @RequestBody PreRegistrationRequest request) {
    return ApiResponse.success(preRegistrationRegisterService.register(request));
  }

  @Override
  @Deprecated
  public ApiResponse<PreRegistrationResponse> cancel(
      @RequestParam("phoneNumber") String phoneNumber) {
    return ApiResponse.success(preRegistrationCancelService.cancel(phoneNumber));
  }

  @Override
  @PostMapping(value = "/coupon-sms")
  public ApiResponse<PreRegistrationCouponSmsResult> sendCouponSms(
      @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
      @RequestParam(defaultValue = "true") boolean dryRun,
      @RequestBody PreRegistrationCouponSmsRequest request) {
    adminApiKeyValidator.validate(adminKey);
    return ApiResponse.success(
        preRegistrationCouponSmsService.send(dryRun, request.messageTemplate()));
  }
}
