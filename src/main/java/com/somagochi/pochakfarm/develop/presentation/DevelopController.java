package com.somagochi.pochakfarm.develop.presentation;

import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.develop.application.DevelopDeviceService;
import com.somagochi.pochakfarm.develop.application.DevelopLoginService;
import com.somagochi.pochakfarm.develop.application.DevelopSampleAnimalService;
import com.somagochi.pochakfarm.develop.dto.DevelopDeviceTokenResponse;
import com.somagochi.pochakfarm.develop.dto.DevelopSampleAnimalResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev")
@Profile({"local", "dev"})
public class DevelopController {

  private final DevelopLoginService developLoginService;
  private final DevelopDeviceService developDeviceService;
  private final DevelopSampleAnimalService developSampleAnimalService;

  @PostMapping("/login/{userId}")
  public ApiResponse<SocialLoginResponse> login(@PathVariable Long userId) {
    return ApiResponse.success(developLoginService.login(userId));
  }

  @PostMapping("/device-token")
  public ApiResponse<DevelopDeviceTokenResponse> issueDeviceToken() {
    return ApiResponse.success(developDeviceService.issueDeviceToken());
  }

  @PostMapping("/animal")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<DevelopSampleAnimalResponse> createSampleAnimal(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(developSampleAnimalService.createSampleAnimal(principal.id()));
  }
}
