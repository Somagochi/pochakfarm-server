package com.somagochi.pochakfarm.storage.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/storage/public/images")
@RequiredArgsConstructor
public class PublicStorageController {

  private final ImageUploadService imageUploadService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<PublicUploadResponse> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam(value = "purpose", required = false) String purpose)
      throws IOException {
    PublicUploadResponse response =
        imageUploadService.uploadPublic(purpose, file.getContentType(), file.getBytes());
    return ApiResponse.success(response);
  }
}
