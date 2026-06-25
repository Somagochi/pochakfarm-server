package com.somagochi.pochakfarm.storage.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.ConfirmRequest;
import com.somagochi.pochakfarm.storage.dto.ConfirmResponse;
import com.somagochi.pochakfarm.storage.dto.PresignRequest;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage/images")
@RequiredArgsConstructor
public class StorageController {

  private final ImageUploadService imageUploadService;

  @PostMapping("/presign")
  public ApiResponse<PresignResponse> presign(
      Authentication authentication, @RequestBody PresignRequest request) {
    Long userId = ((UserPrincipal) authentication.getPrincipal()).id();
    return ApiResponse.success(
        imageUploadService.createPresign(userId, request.purpose(), request.contentType()));
  }

  @PostMapping("/confirm")
  public ApiResponse<ConfirmResponse> confirm(
      Authentication authentication, @RequestBody ConfirmRequest request) {
    Long userId = ((UserPrincipal) authentication.getPrincipal()).id();
    return ApiResponse.success(imageUploadService.confirm(userId, request.key()));
  }
}
