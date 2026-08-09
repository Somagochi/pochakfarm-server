package com.somagochi.pochakfarm.common.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

class CaseInsensitiveEnumConverterFactoryTest {

  private final Converter<String, CardType> converter =
      new CaseInsensitiveEnumConverterFactory().getConverter(CardType.class);

  @Test
  void convertsUpperCase() {
    assertEquals(CardType.SEA, converter.convert("SEA"));
  }

  @Test
  void convertsLowerCase() {
    assertEquals(CardType.SEA, converter.convert("sea"));
  }

  @Test
  void convertsMixedCase() {
    assertEquals(CardType.SEA, converter.convert("Sea"));
  }

  @Test
  void trimsSurroundingWhitespace() {
    assertEquals(CardType.SEA, converter.convert("  sea  "));
  }

  @Test
  void returnsNullForBlankSource() {
    assertNull(converter.convert("   "));
  }

  @Test
  void throwsWithAllowedValuesForUnknownSource() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> converter.convert("river"));

    assertTrue(exception.getMessage().contains("GROUND, SKY, SPACE, SEA"));
  }
}
