package com.somagochi.pochakfarm.characterization.presentation;

import com.somagochi.pochakfarm.characterization.dto.CharacterizationRequestDoc;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationStartResponse;
import com.somagochi.pochakfarm.common.exception.ErrorResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Characterization", description = "사용자-facing 이미지 변환 API")
public interface CharacterizationApiSpec {

  @Operation(
      summary = "이미지 캐릭터라이즈 변환",
      description =
          "홍보 페이지에서 업로드한 이미지를 반려동물 이름과 함께 캐릭터라이즈한다. "
              + "deviceToken 쿠키가 없으면 같은 요청에서 익명 디바이스를 생성하고 Set-Cookie로 내려준다. "
              + "device별 성공 변환은 1회만 허용하며, FAILED 기록만 있는 경우에는 재시도할 수 있다. "
              + "Spring 서버는 원본 이미지를 저장하지 않고 결과 카드 이미지만 S3에 저장한다.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              description = "multipart/form-data 요청. image와 animalName을 함께 전달한다.",
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = CharacterizationRequestDoc.class))))
  @Parameter(
      in = ParameterIn.COOKIE,
      name = "deviceToken",
      required = false,
      description = "익명 디바이스 토큰 쿠키. 없으면 서버가 새로 발급하고 Set-Cookie 응답 헤더를 반환한다.",
      example = "dev_0123456789abcdef0123456789abcdef0123456789abcdef")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description =
          "변환 작업 접수 성공. data.characterizationId에 생성된 캐릭터라이징 ID, "
              + "data.status에 작업 상태, data.cardType에 카드 뒷면 리소스 매칭용 카드 타입 반환")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400",
      description =
          "빈 파일(EMPTY_FILE), 지원하지 않는 이미지 형식(UNSUPPORTED_CONTENT_TYPE), "
              + "또는 비어 있거나 6글자를 초과한 animalName(INVALID_ANIMAL_NAME)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "422",
      description = "이미지 내용상 캐릭터라이징 변환이 불가능함 (UNSUPPORTED_CHARACTERIZATION_IMAGE)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "deviceToken 쿠키가 있지만 DB에 해당 기기가 없음 (DEVICE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "409",
      description =
          "이미 성공 변환을 완료한 기기(CHARACTERIZATION_ALREADY_USED) 또는 처리 중인 요청이 있는 기기"
              + "(CHARACTERIZATION_ALREADY_PROCESSING)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "502",
      description = "Python 변환 서버 호출 실패, 실패 응답, 또는 결과 이미지 디코딩/업로드 실패 (CHARACTERIZATION_FAILED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "503",
      description = "변환 처리 큐가 가득 차 요청을 접수할 수 없음. 잠시 후 재시도 필요 (CHARACTERIZATION_BUSY)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @Deprecated
  ApiResponse<CharacterizationStartResponse> characterize(
      String deviceToken,
      @Parameter(hidden = true) MultipartFile image,
      @Parameter(hidden = true) String animalName,
      @Parameter(hidden = true) HttpServletResponse response);

  @Operation(
      summary = "캐릭터라이징 결과 조회",
      description =
          "characterizationId로 캐릭터라이징 작업 상태와 결과를 조회한다. "
              + "PROCESSING이면 진행 중 상태, SUCCEEDED이면 data.resultImageUrl, "
              + "FAILED이면 data.failureReason을 반환한다. "
              + "일정 시간 이상 PROCESSING에 고착된 작업은 서버가 FAILED로 정리하며, "
              + "이 경우 data.failureReason은 CHARACTERIZATION_TIMED_OUT이다.")
  @Parameter(
      in = ParameterIn.PATH,
      name = "characterizationId",
      required = true,
      description = "조회할 캐릭터라이징 ID",
      example = "1")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description =
          "조회 성공. data.characterizationId, data.status, data.resultImageUrl, data.failureReason 반환")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "404",
      description = "해당 ID의 캐릭터라이징이 없음 (CHARACTERIZATION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @Deprecated
  ApiResponse<CharacterizationResponse> getCharacterization(Long characterizationId);
}
