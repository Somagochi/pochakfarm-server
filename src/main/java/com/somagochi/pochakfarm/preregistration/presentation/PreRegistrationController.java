package com.somagochi.pochakfarm.preregistration.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.device.presentation.DeviceTokenCookieFactory;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationCancelService;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationRegisterService;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationRequest;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @Override
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<PreRegistrationResponse> register(
      @CookieValue(value = DeviceTokenCookieFactory.COOKIE_NAME) String deviceToken,
      @RequestBody PreRegistrationRequest request) {
    return ApiResponse.success(preRegistrationRegisterService.register(deviceToken, request));
  }

  @Override
  @DeleteMapping
  public ApiResponse<PreRegistrationResponse> cancel(
      @CookieValue(value = DeviceTokenCookieFactory.COOKIE_NAME) String deviceToken) {
    return ApiResponse.success(preRegistrationCancelService.cancel(deviceToken));
  }
}
