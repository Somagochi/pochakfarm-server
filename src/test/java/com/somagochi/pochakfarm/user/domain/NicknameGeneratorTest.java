package com.somagochi.pochakfarm.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NicknameGeneratorTest {

  private static final int POOL_SIZE = 100;

  private final RandomProvider randomProvider = mock(RandomProvider.class);
  private final NicknameGenerator nicknameGenerator = new NicknameGenerator(randomProvider);

  @Test
  void combinesAdjectiveNounAndZeroPaddedSuffix() {
    given(randomProvider.nextInt(POOL_SIZE)).willReturn(0, 0, 7);

    assertEquals("행복토끼07", nicknameGenerator.generate());
  }

  @Test
  void usesSuffixWithoutPaddingWhenTwoDigits() {
    given(randomProvider.nextInt(POOL_SIZE)).willReturn(1, 1, 42);

    assertEquals("용감여우42", nicknameGenerator.generate());
  }

  @Test
  void keepsEveryCombinationSixCharactersWithoutWhitespace() {
    for (int index = 0; index < POOL_SIZE; index++) {
      given(randomProvider.nextInt(POOL_SIZE)).willReturn(index, index, index);

      String nickname = nicknameGenerator.generate();

      assertEquals(6, nickname.length(), nickname);
      assertFalse(nickname.contains(" "), nickname);
    }
  }

  @Test
  void holdsHundredDistinctAdjectivesAndNouns() {
    Set<String> adjectives = new HashSet<>();
    Set<String> nouns = new HashSet<>();

    for (int index = 0; index < POOL_SIZE; index++) {
      given(randomProvider.nextInt(POOL_SIZE)).willReturn(index, index, 0);

      String nickname = nicknameGenerator.generate();
      adjectives.add(nickname.substring(0, 2));
      nouns.add(nickname.substring(2, 4));
    }

    assertEquals(POOL_SIZE, adjectives.size());
    assertEquals(POOL_SIZE, nouns.size());
  }
}
