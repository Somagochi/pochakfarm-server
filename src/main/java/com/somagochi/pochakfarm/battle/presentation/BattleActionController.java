package com.somagochi.pochakfarm.battle.presentation;

import com.somagochi.pochakfarm.battle.application.BattleActionService;
import com.somagochi.pochakfarm.battle.application.BattleFinalRoundService;
import com.somagochi.pochakfarm.battle.application.BattleStateQueryService;
import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleActionResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultRequest;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStartResponse;
import com.somagochi.pochakfarm.battle.dto.BattleStateResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/battles")
@RequiredArgsConstructor
public class BattleActionController implements BattleActionApiSpec {

  private final BattleActionService battleActionService;
  private final BattleFinalRoundService battleFinalRoundService;
  private final BattleStateQueryService battleStateQueryService;

  @Override
  @PostMapping("/{battleId}/actions")
  public ApiResponse<BattleActionResponse> selectSkill(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long battleId,
      @RequestBody BattleActionRequest request) {
    return ApiResponse.success(battleActionService.selectSkill(principal.id(), battleId, request));
  }

  @Override
  @GetMapping("/{battleId}")
  public ApiResponse<BattleStateResponse> getBattle(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long battleId) {
    return ApiResponse.success(battleStateQueryService.getBattle(principal.id(), battleId));
  }

  @Override
  @PostMapping("/{battleId}/final-round/start")
  public ApiResponse<BattleFinalRoundStartResponse> startFinalRound(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long battleId) {
    return ApiResponse.success(battleFinalRoundService.start(principal.id(), battleId));
  }

  @Override
  @PostMapping("/{battleId}/final-round/result")
  public ApiResponse<BattleFinalRoundResultResponse> submitFinalRound(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long battleId,
      @RequestBody BattleFinalRoundResultRequest request) {
    return ApiResponse.success(battleFinalRoundService.submit(principal.id(), battleId, request));
  }
}
