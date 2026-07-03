package com.somagochi.pochakfarm.preregistration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.somagochi.pochakfarm.common.properties.PreRegistrationCryptoProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PreRegistrationCryptoServiceTest {

  private static final String PHONE = "01012345678";

  private final PreRegistrationCryptoService cryptoService =
      new PreRegistrationCryptoService(
          new PreRegistrationCryptoProperties(
              "01234567890123456789012345678901", "hash-key-for-pre-registration-phone-number"));

  @Test
  void encryptsAndDecryptsPhoneNumber() {
    String encrypted = cryptoService.encrypt(PHONE);

    assertNotEquals(PHONE, encrypted);
    assertEquals(PHONE, cryptoService.decrypt(encrypted));
  }

  @Test
  void createsDifferentCipherTextsForSamePhoneNumber() {
    String first = cryptoService.encrypt(PHONE);
    String second = cryptoService.encrypt(PHONE);

    assertNotEquals(first, second);
  }

  @Test
  void createsStableHashForSamePhoneNumber() {
    String first = cryptoService.hash(PHONE);
    String second = cryptoService.hash(PHONE);

    assertEquals(first, second);
  }

  @Test
  void acceptsBase64PrefixedKeys() {
    String encodedEncryptionKey =
        Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
    String encodedHashKey =
        Base64.getEncoder()
            .encodeToString(
                "hash-key-for-pre-registration-phone-number".getBytes(StandardCharsets.UTF_8));
    PreRegistrationCryptoService prefixedCryptoService =
        new PreRegistrationCryptoService(
            new PreRegistrationCryptoProperties(
                "base64:" + encodedEncryptionKey, "base64:" + encodedHashKey));

    String encrypted = prefixedCryptoService.encrypt(PHONE);

    assertEquals(PHONE, prefixedCryptoService.decrypt(encrypted));
  }
}
