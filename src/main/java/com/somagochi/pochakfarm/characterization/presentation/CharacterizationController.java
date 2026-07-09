package com.somagochi.pochakfarm.characterization.presentation;

import com.somagochi.pochakfarm.characterization.application.CharacterizationService;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.device.application.DeviceService;
import com.somagochi.pochakfarm.device.application.DeviceService.DeviceResolution;
import com.somagochi.pochakfarm.device.presentation.DeviceTokenCookieFactory;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/characterizations")
@RequiredArgsConstructor
public class CharacterizationController implements CharacterizationApiSpec {

  private final CharacterizationService characterizationService;
  private final DeviceService deviceService;
  private final DeviceTokenCookieFactory deviceTokenCookieFactory;

  @Override
  @PostMapping(value = "/public", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CharacterizationResponse> characterize(
      @CookieValue(value = DeviceTokenCookieFactory.COOKIE_NAME, required = false)
          String deviceToken,
      @RequestParam("image") MultipartFile image,
      @RequestParam("animalName") String animalName,
      HttpServletResponse response) {
    DeviceResolution resolution = deviceService.resolveOrIssue(deviceToken);
    if (resolution.issued()) {
      response.addHeader(
          HttpHeaders.SET_COOKIE,
          deviceTokenCookieFactory.create(resolution.device().getDeviceToken()).toString());
    }
    return ApiResponse.success(
        characterizationService.characterize(resolution.device().getId(), image, animalName));
  }
}
