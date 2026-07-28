package com.somagochi.pochakfarm.capture.presentation;

import com.somagochi.pochakfarm.capture.application.CaptureStartService;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captures")
@RequiredArgsConstructor
public class CaptureController implements CaptureApiSpec {

  private final CaptureStartService captureStartService;

  @Override
  @PostMapping
  public ApiResponse<CaptureStartResponse> startCapture(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody CaptureStartRequest request) {
    return ApiResponse.success(captureStartService.startCapture(principal.id(), request));
  }
}
