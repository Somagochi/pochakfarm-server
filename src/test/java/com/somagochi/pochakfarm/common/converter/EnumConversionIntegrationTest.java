package com.somagochi.pochakfarm.common.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.format.support.FormattingConversionService;

@SpringBootTest
class EnumConversionIntegrationTest {

  @Autowired
  @Qualifier("mvcConversionService")
  private FormattingConversionService conversionService;

  @Test
  void mvcConversionServiceConvertsLowerCaseEnum() {
    assertEquals(CardType.SEA, conversionService.convert("sea", CardType.class));
    assertEquals(CardType.GROUND, conversionService.convert("Ground", CardType.class));
  }

  @Test
  void mvcConversionServiceStillConvertsUpperCaseEnum() {
    assertEquals(CardType.SEA, conversionService.convert("SEA", CardType.class));
  }
}
