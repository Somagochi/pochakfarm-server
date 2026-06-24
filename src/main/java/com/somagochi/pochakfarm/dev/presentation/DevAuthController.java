package com.somagochi.pochakfarm.dev.presentation;

import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.dev.application.DevLoginService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@Profile({"local", "dev"})
public class DevAuthController {

  private final DevLoginService devLoginService;

  public DevAuthController(DevLoginService devLoginService) {
    this.devLoginService = devLoginService;
  }

  @PostMapping("/login/{userId}")
  public ApiResponse<SocialLoginResponse> login(@PathVariable Long userId) {
    return ApiResponse.success(devLoginService.login(userId));
  }
}
