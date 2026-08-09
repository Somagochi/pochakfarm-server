package com.somagochi.pochakfarm.storage.domain;

import java.time.Duration;

public interface FileStorage {

  /** 지정한 key 로 업로드할 수 있는 presigned PUT URL 을 발급한다. content-type 은 URL 에 고정된다. */
  PresignedUpload presignPut(String key, String contentType, Duration ttl);

  /** 지정한 key 를 다운로드할 수 있는 presigned GET URL 을 발급한다. */
  PresignedUpload presignGet(String key, Duration ttl);

  /** 서버가 가진 바이트를 직접 스토리지에 업로드한다(서버 프록시 업로드). */
  void upload(String key, String contentType, byte[] content);

  /** 오브젝트 메타데이터를 조회한다. 존재하지 않으면 예외를 던진다. */
  StoredObject head(String key);

  /** 같은 버킷 안에서 오브젝트를 복사한다. 원본이 존재하지 않으면 예외를 던진다. */
  void copy(String sourceKey, String targetKey);

  /** 오브젝트에 접근 가능한 URL 을 구성한다. */
  String buildUrl(String key);
}
