package com.somagochi.pochakfarm.share.presentation;

import com.somagochi.pochakfarm.characterization.application.CharacterizationReadService;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.common.properties.PromotionProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

  private final CharacterizationReadService characterizationReadService;
  private final PromotionProperties promotionProperties;

  @GetMapping("/characterizations/{characterizationId}")
  public ModelAndView share(@PathVariable Long characterizationId, HttpServletRequest request) {
    CharacterizationResponse characterization =
        characterizationReadService.getCharacterization(characterizationId);

    ModelAndView modelAndView = new ModelAndView("share/characterization");
    modelAndView.addObject("imageUrl", characterization.resultImageUrl());
    modelAndView.addObject("pageUrl", buildOgUrl(request, characterizationId));
    modelAndView.addObject("redirectUrl", buildRedirectUrl(characterizationId));
    return modelAndView;
  }

  private String buildRedirectUrl(Long characterizationId) {
    return promotionProperties.baseUrl() + "/result?characterization_id=" + characterizationId;
  }

  private String buildOgUrl(HttpServletRequest request, Long characterizationId) {
    String scheme = headerOrDefault(request, "X-Forwarded-Proto", request.getScheme());
    String host = headerOrDefault(request, "X-Forwarded-Host", request.getHeader("Host"));
    if (!StringUtils.hasText(host)) {
      host = request.getServerName();
    }
    return scheme + "://" + host + "/share/characterizations/" + characterizationId;
  }

  private String headerOrDefault(HttpServletRequest request, String header, String defaultValue) {
    String value = request.getHeader(header);
    return StringUtils.hasText(value) ? value : defaultValue;
  }
}
