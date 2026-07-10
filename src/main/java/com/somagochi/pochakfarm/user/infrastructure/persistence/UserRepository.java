package com.somagochi.pochakfarm.user.infrastructure.persistence;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  User save(User user);

  Optional<User> findById(Long id);

  Optional<User> findBySocialAccountProviderAndSocialAccountProviderId(
      SocialProvider provider, String providerId);
}
