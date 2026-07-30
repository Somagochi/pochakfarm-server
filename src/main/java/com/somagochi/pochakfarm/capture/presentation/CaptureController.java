package com.somagochi.pochakfarm.capture.presentation;

import com.somagochi.pochakfarm.capture.application.CaptureCompleteService;
import com.somagochi.pochakfarm.capture.application.CaptureQueryService;
import com.somagochi.pochakfarm.capture.application.CaptureStartService;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captures")
@RequiredArgsConstructor
public class CaptureController implements CaptureApiSpec {

  private final CaptureStartService captureStartService;
  private final CaptureCompleteService captureCompleteService;
  private final CaptureQueryService captureQueryService;

  @Override
  @PostMapping
  public ApiResponse<CaptureStartResponse> startCapture(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody CaptureStartRequest request) {
    return ApiResponse.success(captureStartService.startCapture(principal.id(), request));
  }

  @Override
  @PostMapping("/{captureId}/original-image/complete")
  public ResponseEntity<ApiResponse<CaptureCompleteResponse>> completeOriginalImage(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long captureId) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(
            ApiResponse.success(
                captureCompleteService.completeOriginalImage(principal.id(), captureId)));
  }

  @Override
  @GetMapping("/{captureId}")
  public ApiResponse<CaptureResponse> getCapture(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long captureId) {
    return ApiResponse.success(captureQueryService.getCapture(principal.id(), captureId));
  }
}
