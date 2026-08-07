package com.somagochi.pochakfarm.capture.presentation;

import com.somagochi.pochakfarm.capture.application.CaptureAnimalService;
import com.somagochi.pochakfarm.capture.application.CaptureAttemptPurchaseService;
import com.somagochi.pochakfarm.capture.application.CaptureAvailabilityService;
import com.somagochi.pochakfarm.capture.application.CaptureCompleteService;
import com.somagochi.pochakfarm.capture.application.CaptureGameResultService;
import com.somagochi.pochakfarm.capture.application.CaptureOverviewService;
import com.somagochi.pochakfarm.capture.application.CaptureQueryService;
import com.somagochi.pochakfarm.capture.application.CaptureStartService;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAnimalPlacementResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureCompleteResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureOverviewResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureStartResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captures")
@RequiredArgsConstructor
public class CaptureController implements CaptureApiSpec {

  private final CaptureStartService captureStartService;
  private final CaptureAttemptPurchaseService captureAttemptPurchaseService;
  private final CaptureAvailabilityService captureAvailabilityService;
  private final CaptureAnimalService captureAnimalService;
  private final CaptureCompleteService captureCompleteService;
  private final CaptureGameResultService captureGameResultService;
  private final CaptureOverviewService captureOverviewService;
  private final CaptureQueryService captureQueryService;

  @Override
  @PostMapping
  public ApiResponse<CaptureStartResponse> startCapture(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody CaptureStartRequest request) {
    return ApiResponse.success(captureStartService.startCapture(principal.id(), request));
  }

  @Override
  @GetMapping("/availability")
  public ApiResponse<CaptureAvailabilityResponse> getAvailability(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(captureAvailabilityService.getAvailability(principal.id()));
  }

  @Override
  @PostMapping("/attempts/purchase")
  public ApiResponse<CaptureAttemptPurchaseResponse> purchaseAttempt(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody CaptureAttemptPurchaseRequest request) {
    return ApiResponse.success(captureAttemptPurchaseService.purchase(principal.id(), request));
  }

  @Override
  @GetMapping("/overview")
  public ApiResponse<CaptureOverviewResponse> getOverview(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(captureOverviewService.getOverview(principal.id()));
  }

  @Override
  @PostMapping("/{captureId}/original-image/complete")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<CaptureCompleteResponse> completeOriginalImage(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long captureId) {
    return ApiResponse.success(
        captureCompleteService.completeOriginalImage(principal.id(), captureId));
  }

  @Override
  @PostMapping("/{captureId}/game-result")
  public ApiResponse<CaptureGameResultResponse> submitGameResult(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long captureId,
      @RequestBody CaptureGameResultRequest request) {
    return ApiResponse.success(captureGameResultService.submit(principal.id(), captureId, request));
  }

  @Override
  @GetMapping("/{captureId}")
  public ApiResponse<CaptureResponse> getCapture(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long captureId) {
    return ApiResponse.success(captureQueryService.getCapture(principal.id(), captureId));
  }

  @Override
  @PostMapping("/{captureId}/animal")
  public ApiResponse<CaptureAnimalPlacementResponse> placeAnimal(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long captureId,
      @RequestBody CaptureAnimalPlacementRequest request) {
    return ApiResponse.success(captureAnimalService.place(principal.id(), captureId, request));
  }
}
