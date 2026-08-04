package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.LevelRewardPolicy;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserProfileResponse;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

  @Mock private UserRepository userRepository;

  @Test
  void returnsProfileWithCurrentAndRequiredExperienceWithoutChangingUser() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
    user.changeNickname("포착이");
    ReflectionTestUtils.setField(user, "level", 3);
    ReflectionTestUtils.setField(user, "experience", 54L);
    given(userRepository.findById(1L)).willReturn(Optional.of(user));
    UserQueryService service = new UserQueryService(userRepository, new LevelRewardPolicy());

    UserProfileResponse response = service.getProfile(1L);

    assertEquals("포착이", response.nickname());
    assertEquals(3, response.level());
    assertEquals(1000, response.coins());
    assertEquals(54, response.currentExperience());
    assertEquals(60, response.requiredExperience());
    assertEquals(6, response.remainingExperience());
    assertEquals(3, user.getLevel());
    assertEquals(54, user.getExperience());
  }
}
