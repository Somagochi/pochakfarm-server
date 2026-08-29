package com.somagochi.pochakfarm.develop.presentation;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.develop.application.DevelopAchievementAssetService;
import com.somagochi.pochakfarm.develop.application.DevelopGymLeaderAssetService;
import com.somagochi.pochakfarm.develop.dto.DevelopAssetPresignRequest;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
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
@RequestMapping("/api/dev/assets")
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevelopAssetController {

  private static final String ACHIEVEMENT_TAB = "achievement";
  private static final String GYM_LEADER_TAB = "gym-leader";

  private final DevelopAchievementAssetService developAchievementAssetService;
  private final DevelopGymLeaderAssetService developGymLeaderAssetService;
  private final ImageUploadService imageUploadService;

  @GetMapping
  public ModelAndView assets(@RequestParam(defaultValue = ACHIEVEMENT_TAB) String tab) {
    ModelAndView modelAndView = new ModelAndView("develop/assets");
    modelAndView.addObject("tab", GYM_LEADER_TAB.equals(tab) ? GYM_LEADER_TAB : ACHIEVEMENT_TAB);
    modelAndView.addObject("achievements", developAchievementAssetService.getAssets());
    modelAndView.addObject("gymLeaders", developGymLeaderAssetService.getAssets());
    return modelAndView;
  }

  @PostMapping("/presign")
  @ResponseBody
  public ApiResponse<PresignResponse> presign(@RequestBody DevelopAssetPresignRequest request) {
    return ApiResponse.success(
        imageUploadService.createPublicPresign(request.target().purpose(), request.contentType()));
  }

  @PostMapping("/achievements/{achievementId}/images")
  public String updateAchievementImages(
      @PathVariable Long achievementId,
      @RequestParam(required = false) String unachievedImageKey,
      @RequestParam(required = false) String unachievedImageContentType,
      @RequestParam(required = false) String achievedImageKey,
      @RequestParam(required = false) String achievedImageContentType,
      RedirectAttributes redirectAttributes) {
    return handle(
        ACHIEVEMENT_TAB,
        redirectAttributes,
        () ->
            developAchievementAssetService.updateAchievementImages(
                achievementId,
                unachievedImageKey,
                unachievedImageContentType,
                achievedImageKey,
                achievedImageContentType));
  }

  @PostMapping("/achievements/rewards/{rewardId}/amount")
  public String updateRewardAmount(
      @PathVariable Long rewardId,
      @RequestParam long amount,
      RedirectAttributes redirectAttributes) {
    return handle(
        ACHIEVEMENT_TAB,
        redirectAttributes,
        () -> developAchievementAssetService.updateRewardAmount(rewardId, amount));
  }

  @PostMapping("/achievements/rewards/{rewardId}/badge-image")
  public String updateRewardBadgeImage(
      @PathVariable Long rewardId,
      @RequestParam String badgeImageKey,
      @RequestParam String badgeImageContentType,
      RedirectAttributes redirectAttributes) {
    return handle(
        ACHIEVEMENT_TAB,
        redirectAttributes,
        () ->
            developAchievementAssetService.updateRewardBadgeImage(
                rewardId, badgeImageKey, badgeImageContentType));
  }

  @PostMapping("/gym-leaders/{gymLeaderId}/image")
  public String updateGymLeaderImage(
      @PathVariable Long gymLeaderId,
      @RequestParam String imageKey,
      @RequestParam String imageContentType,
      RedirectAttributes redirectAttributes) {
    return handle(
        GYM_LEADER_TAB,
        redirectAttributes,
        () ->
            developGymLeaderAssetService.updateGymLeaderImage(
                gymLeaderId, imageKey, imageContentType));
  }

  @PostMapping("/gym-leaders/{gymLeaderId}/badge-image")
  public String updateGymLeaderBadgeImage(
      @PathVariable Long gymLeaderId,
      @RequestParam String badgeImageKey,
      @RequestParam String badgeImageContentType,
      RedirectAttributes redirectAttributes) {
    return handle(
        GYM_LEADER_TAB,
        redirectAttributes,
        () ->
            developGymLeaderAssetService.updateGymLeaderBadgeImage(
                gymLeaderId, badgeImageKey, badgeImageContentType));
  }

  @PostMapping("/gym-leaders/animals/{gymLeaderAnimalId}/image")
  public String updateGymLeaderAnimalImage(
      @PathVariable Long gymLeaderAnimalId,
      @RequestParam String imageKey,
      @RequestParam String imageContentType,
      RedirectAttributes redirectAttributes) {
    return handle(
        GYM_LEADER_TAB,
        redirectAttributes,
        () ->
            developGymLeaderAssetService.updateGymLeaderAnimalImage(
                gymLeaderAnimalId, imageKey, imageContentType));
  }

  private String handle(String tab, RedirectAttributes redirectAttributes, Runnable action) {
    try {
      action.run();
      redirectAttributes.addFlashAttribute("message", "저장했습니다.");
    } catch (BusinessException e) {
      log.error("에셋 저장 실패: {} - {}", e.getCode(), e.getMessage(), e);
      redirectAttributes.addFlashAttribute("error", e.getCode() + ": " + e.getMessage());
    }
    return "redirect:/api/dev/assets?tab=" + tab;
  }
}
