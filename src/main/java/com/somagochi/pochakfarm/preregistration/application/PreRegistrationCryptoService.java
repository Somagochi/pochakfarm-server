package com.somagochi.pochakfarm.preregistration.application;

import com.somagochi.pochakfarm.common.properties.PreRegistrationCryptoProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PreRegistrationCryptoService {

  private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final String AES_ALGORITHM = "AES";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String CIPHER_TEXT_PREFIX = "v1:";
  private static final String BASE64_KEY_PREFIX = "base64:";
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int IV_LENGTH_BYTES = 12;

  private final SecretKeySpec encryptionKey;
  private final SecretKeySpec hashKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public PreRegistrationCryptoService(PreRegistrationCryptoProperties properties) {
    byte[] encryptionKeyBytes = decodeConfiguredKey(properties.encryptionKey());
    validateAesKeyLength(encryptionKeyBytes);
    this.encryptionKey = new SecretKeySpec(encryptionKeyBytes, AES_ALGORITHM);
    this.hashKey = new SecretKeySpec(decodeConfiguredKey(properties.hashKey()), HMAC_ALGORITHM);
  }

  public String encrypt(String plainText) {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(
          Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
      buffer.put(iv);
      buffer.put(encrypted);
      return CIPHER_TEXT_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Failed to encrypt pre-registration phone number", exception);
    }
  }

  public String decrypt(String cipherText) {
    try {
      String encoded =
          cipherText.startsWith(CIPHER_TEXT_PREFIX)
              ? cipherText.substring(CIPHER_TEXT_PREFIX.length())
              : cipherText;
      byte[] payload = Base64.getDecoder().decode(encoded);
      byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
      byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(
          Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalStateException("Failed to decrypt pre-registration phone number", exception);
    }
  }

  public String hash(String plainText) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hashKey);
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Failed to hash pre-registration phone number", exception);
    }
  }

  private byte[] decodeConfiguredKey(String configuredKey) {
    if (configuredKey == null || configuredKey.isBlank()) {
      throw new IllegalStateException("Pre-registration crypto keys must be configured");
    }
    if (configuredKey.startsWith(BASE64_KEY_PREFIX)) {
      return Base64.getDecoder().decode(configuredKey.substring(BASE64_KEY_PREFIX.length()));
    }
    return configuredKey.getBytes(StandardCharsets.UTF_8);
  }

  private void validateAesKeyLength(byte[] key) {
    if (key.length != 16 && key.length != 24 && key.length != 32) {
      throw new IllegalStateException(
          "Pre-registration encryption key must be 16, 24, or 32 bytes");
    }
  }
}
