package com.somagochi.pochakfarm.battle.presentation;

import com.somagochi.pochakfarm.battle.application.BattleStartService;
import com.somagochi.pochakfarm.battle.application.GymLeaderQueryService;
import com.somagochi.pochakfarm.battle.dto.BattleStartRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderDetailResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
public class BattleController implements BattleApiSpec {

  private final GymLeaderQueryService gymLeaderQueryService;
  private final BattleStartService battleStartService;
  private final Clock clock;

  @Override
  @GetMapping("/gym-leaders")
  public ApiResponse<List<GymLeaderResponse>> getGymLeaders(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(gymLeaderQueryService.getGymLeaders(principal.id()));
  }

  @Override
  @GetMapping("/gym-leaders/{gymLeaderId}")
  public ApiResponse<GymLeaderDetailResponse> getGymLeader(
      @PathVariable Long gymLeaderId, @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(gymLeaderQueryService.getGymLeader(principal.id(), gymLeaderId));
  }

  @Override
  @PostMapping
  public ApiResponse<BattleStartResponse> startBattle(
      @RequestBody BattleStartRequest request, @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success(
        battleStartService.start(principal.id(), request, Instant.now(clock)));
  }
}
