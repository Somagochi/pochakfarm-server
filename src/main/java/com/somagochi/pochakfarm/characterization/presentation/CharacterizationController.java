package com.somagochi.pochakfarm.characterization.presentation;

import com.somagochi.pochakfarm.characterization.application.CharacterizationReadService;
import com.somagochi.pochakfarm.characterization.application.CharacterizationService;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationStartResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.device.application.DeviceService;
import com.somagochi.pochakfarm.device.application.DeviceService.DeviceResolution;
import com.somagochi.pochakfarm.device.presentation.DeviceTokenCookieFactory;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/characterizations")
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.characterization",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class CharacterizationController implements CharacterizationApiSpec {

  private final CharacterizationService characterizationService;
  private final CharacterizationReadService characterizationReadService;
  private final DeviceService deviceService;
  private final DeviceTokenCookieFactory deviceTokenCookieFactory;

  @Override
  @PostMapping(value = "/public", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CharacterizationStartResponse> characterize(
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

  @Override
  @GetMapping("/public/{characterizationId}")
  public ApiResponse<CharacterizationResponse> getCharacterization(
      @PathVariable Long characterizationId) {
    return ApiResponse.success(characterizationReadService.getCharacterization(characterizationId));
  }
}
