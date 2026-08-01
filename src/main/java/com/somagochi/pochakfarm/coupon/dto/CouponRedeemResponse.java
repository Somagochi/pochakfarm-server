package com.somagochi.pochakfarm.coupon.dto;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record CouponRedeemResponse(
    @Schema(description = "지급된 카드(포획) ID", example = "1") Long captureId,
    @Schema(description = "동물 이름", example = "두부") String animalName,
    @Schema(description = "카드 타입", example = "SEA") CardType cardType,
    @Schema(description = "카드 티어", example = "S") Tier tier,
    @Schema(description = "카드 번호", example = "123") String cardNo,
    @Schema(description = "카드 이미지 URL") String cardImageUrl,
    @Schema(description = "누끼(동물) 이미지 업로드용 presign 정보") PresignResponse animalImageUpload) {

  public static CouponRedeemResponse of(
      Capture capture, String cardImageUrl, PresignResponse animalImageUpload) {
    return new CouponRedeemResponse(
        capture.getId(),
        capture.getAnimalName(),
        capture.getCardType(),
        capture.getTier(),
        capture.getCardNo(),
        cardImageUrl,
        animalImageUpload);
  }
}
