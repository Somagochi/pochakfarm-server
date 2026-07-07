package com.somagochi.pochakfarm.develop.presentation;

import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.develop.application.DevelopDeviceService;
import com.somagochi.pochakfarm.develop.application.DevelopLoginService;
import com.somagochi.pochakfarm.develop.dto.DevelopDeviceTokenResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@Profile({"local", "dev"})
public class DevelopController {

  private final DevelopLoginService developLoginService;
  private final DevelopDeviceService developDeviceService;

  public DevelopController(
      DevelopLoginService developLoginService, DevelopDeviceService developDeviceService) {
    this.developLoginService = developLoginService;
    this.developDeviceService = developDeviceService;
  }

  @PostMapping("/login/{userId}")
  public ApiResponse<SocialLoginResponse> login(@PathVariable Long userId) {
    return ApiResponse.success(developLoginService.login(userId));
  }

  @PostMapping("/device-token")
  public ApiResponse<DevelopDeviceTokenResponse> issueDeviceToken() {
    return ApiResponse.success(developDeviceService.issueDeviceToken());
  }
}
