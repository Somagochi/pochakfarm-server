package com.somagochi.pochakfarm.storage.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.S3Properties;
import com.somagochi.pochakfarm.storage.dto.ConfirmResponse;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import com.somagochi.pochakfarm.storage.infrastructure.InMemoryFileStorage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImageUploadServiceTest {

  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

  private final S3Properties properties =
      new S3Properties(
          "test-bucket",
          "ap-northeast-2",
          null,
          null,
          Duration.ofMinutes(5),
          MAX_FILE_SIZE,
          List.of("image/jpeg", "image/png", "image/webp"));
  private final InMemoryFileStorage fileStorage = new InMemoryFileStorage();
  private final ImageUploadService service = new ImageUploadService(fileStorage, properties);

  @Test
  void createsPresignWithOwnerScopedKey() {
    PresignResponse response = service.createPresign(42L, "profile", "image/png");

    assertTrue(response.key().matches("images/profile/42/[a-z0-9-]+\\.png"));
    assertTrue(response.uploadUrl().endsWith(response.key()));
    assertEquals("https://upload.test/" + response.key(), response.uploadUrl());
  }

  @Test
  void usesEtcWhenPurposeIsBlank() {
    PresignResponse response = service.createPresign(7L, "  ", "image/jpeg");

    assertTrue(response.key().startsWith("images/etc/7/"));
    assertTrue(response.key().endsWith(".jpg"));
  }

  @Test
  void rejectsUnsupportedContentTypeOnPresign() {
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.createPresign(1L, "profile", "image/gif"));

    assertEquals(ErrorCode.UNSUPPORTED_CONTENT_TYPE.getCode(), exception.getCode());
  }

  @Test
  void rejectsInvalidPurpose() {
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.createPresign(1L, "../etc", "image/png"));

    assertEquals(ErrorCode.INVALID_UPLOAD_PURPOSE.getCode(), exception.getCode());
  }

  @Test
  void confirmReturnsUrlWhenObjectIsValid() {
    String key = "images/profile/42/photo.png";
    fileStorage.put(key, 1024, "image/png");

    ConfirmResponse response = service.confirm(42L, key);

    assertEquals(key, response.key());
    assertEquals("https://cdn.test/" + key, response.url());
  }

  @Test
  void confirmRejectsKeyOwnedByAnotherUser() {
    String key = "images/profile/99/photo.png";
    fileStorage.put(key, 1024, "image/png");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.confirm(42L, key));

    assertEquals(ErrorCode.FORBIDDEN_FILE_ACCESS.getCode(), exception.getCode());
  }

  @Test
  void confirmRejectsMissingObject() {
    String key = "images/profile/42/missing.png";

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.confirm(42L, key));

    assertEquals(ErrorCode.FILE_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void confirmRejectsContentTypeMismatch() {
    String key = "images/profile/42/fake.png";
    fileStorage.put(key, 1024, "application/zip");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.confirm(42L, key));

    assertEquals(ErrorCode.UNSUPPORTED_CONTENT_TYPE.getCode(), exception.getCode());
  }

  @Test
  void confirmRejectsOversizedObject() {
    String key = "images/profile/42/big.png";
    fileStorage.put(key, MAX_FILE_SIZE + 1, "image/png");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.confirm(42L, key));

    assertEquals(ErrorCode.FILE_TOO_LARGE.getCode(), exception.getCode());
  }

  @Test
  void uploadPublicStoresUnderPublicPrefixAndReturnsUrl() {
    byte[] content = "fake-image".getBytes(StandardCharsets.UTF_8);

    PublicUploadResponse response = service.uploadPublic("pre-apply", "image/png", content);

    assertTrue(response.key().matches("public/pre-apply/[a-z0-9-]+\\.png"));
    assertEquals("https://cdn.test/" + response.key(), response.url());
  }

  @Test
  void uploadPublicUsesEtcWhenPurposeIsBlank() {
    byte[] content = "fake-image".getBytes(StandardCharsets.UTF_8);

    PublicUploadResponse response = service.uploadPublic(null, "image/jpeg", content);

    assertTrue(response.key().startsWith("public/etc/"));
    assertTrue(response.key().endsWith(".jpg"));
  }

  @Test
  void uploadPublicRejectsUnsupportedContentType() {
    byte[] content = "fake-image".getBytes(StandardCharsets.UTF_8);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.uploadPublic("pre-apply", "image/gif", content));

    assertEquals(ErrorCode.UNSUPPORTED_CONTENT_TYPE.getCode(), exception.getCode());
  }

  @Test
  void uploadPublicRejectsEmptyContent() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.uploadPublic("pre-apply", "image/png", new byte[0]));

    assertEquals(ErrorCode.EMPTY_FILE.getCode(), exception.getCode());
  }

  @Test
  void uploadPublicRejectsOversizedContent() {
    byte[] content = new byte[(int) MAX_FILE_SIZE + 1];

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.uploadPublic("pre-apply", "image/png", content));

    assertEquals(ErrorCode.FILE_TOO_LARGE.getCode(), exception.getCode());
  }
}
