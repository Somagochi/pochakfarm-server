package com.somagochi.pochakfarm.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class NicknameGeneratorTest {

  private final RandomProvider randomProvider = mock(RandomProvider.class);
  private final NicknameGenerator nicknameGenerator = new NicknameGenerator(randomProvider);

  @Test
  void combinesAdjectiveNounAndZeroPaddedSuffix() {
    given(randomProvider.nextInt(50)).willReturn(0, 0);
    given(randomProvider.nextInt(100)).willReturn(7);

    assertEquals("행복토끼07", nicknameGenerator.generate());
  }

  @Test
  void usesSuffixWithoutPaddingWhenTwoDigits() {
    given(randomProvider.nextInt(50)).willReturn(1, 1);
    given(randomProvider.nextInt(100)).willReturn(42);

    assertEquals("용감여우42", nicknameGenerator.generate());
  }

  @Test
  void generatesSixCharacterNicknameWithoutWhitespaceForEveryCombination() {
    RandomProvider sequential = mock(RandomProvider.class);
    NicknameGenerator generator = new NicknameGenerator(sequential);

    IntStream.range(0, 50)
        .forEach(
            index -> {
              given(sequential.nextInt(50)).willReturn(index, index);
              given(sequential.nextInt(100)).willReturn(index);

              String nickname = generator.generate();

              assertEquals(6, nickname.length(), nickname);
              assertFalse(nickname.contains(" "), nickname);
            });
  }
}
