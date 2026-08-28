package com.somagochi.pochakfarm.develop.presentation;

import com.somagochi.pochakfarm.achievement.application.AchievementReconciliationService;
import com.somagochi.pochakfarm.achievement.dto.AchievementReconciliationResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.develop.application.DevelopAchievementAssetService;
import com.somagochi.pochakfarm.develop.dto.DevelopAchievementAssetPresignRequest;
import com.somagochi.pochakfarm.develop.dto.DevelopAchievementReconciliationRequest;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/api/dev/achievements")
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevelopAchievementAssetController {

  // TODO: 운영 환경에서 대사를 실행하려면 /api/admin 전용 인증·인가 구성이 필요하다.
  private final DevelopAchievementAssetService developAchievementAssetService;
  private final AchievementReconciliationService achievementReconciliationService;

  @GetMapping
  public ModelAndView assets() {
    ModelAndView modelAndView = new ModelAndView("develop/achievement-assets");
    modelAndView.addObject("achievements", developAchievementAssetService.getAssets());
    return modelAndView;
  }

  @PostMapping("/presign")
  @ResponseBody
  public ApiResponse<PresignResponse> presign(
      @RequestBody DevelopAchievementAssetPresignRequest request) {
    if (request.isBadgeTarget()) {
      return ApiResponse.success(
          developAchievementAssetService.presignBadgeImage(request.contentType()));
    }
    return ApiResponse.success(
        developAchievementAssetService.presignAchievementImage(request.contentType()));
  }

  @PostMapping("/reconciliation")
  @ResponseBody
  public ApiResponse<AchievementReconciliationResult> reconcile(
      @RequestBody DevelopAchievementReconciliationRequest request) {
    return ApiResponse.success(achievementReconciliationService.reconcile(request.userIds()));
  }

  @PostMapping("/{achievementId}/images")
  public String updateAchievementImages(
      @PathVariable Long achievementId,
      @RequestParam(required = false) String unachievedImageKey,
      @RequestParam(required = false) String unachievedImageContentType,
      @RequestParam(required = false) String achievedImageKey,
      @RequestParam(required = false) String achievedImageContentType,
      RedirectAttributes redirectAttributes) {
    return handle(
        redirectAttributes,
        () ->
            developAchievementAssetService.updateAchievementImages(
                achievementId,
                unachievedImageKey,
                unachievedImageContentType,
                achievedImageKey,
                achievedImageContentType));
  }

  @PostMapping("/rewards/{rewardId}/amount")
  public String updateRewardAmount(
      @PathVariable Long rewardId,
      @RequestParam long amount,
      RedirectAttributes redirectAttributes) {
    return handle(
        redirectAttributes,
        () -> developAchievementAssetService.updateRewardAmount(rewardId, amount));
  }

  @PostMapping("/rewards/{rewardId}/badge-image")
  public String updateRewardBadgeImage(
      @PathVariable Long rewardId,
      @RequestParam String badgeImageKey,
      @RequestParam String badgeImageContentType,
      RedirectAttributes redirectAttributes) {
    return handle(
        redirectAttributes,
        () ->
            developAchievementAssetService.updateRewardBadgeImage(
                rewardId, badgeImageKey, badgeImageContentType));
  }

  private String handle(RedirectAttributes redirectAttributes, Runnable action) {
    try {
      action.run();
      redirectAttributes.addFlashAttribute("message", "저장했습니다.");
    } catch (BusinessException e) {
      log.error("업적 에셋 저장 실패: {} - {}", e.getCode(), e.getMessage(), e);
      redirectAttributes.addFlashAttribute("error", e.getCode() + ": " + e.getMessage());
    }
    return "redirect:/api/dev/achievements";
  }
}
