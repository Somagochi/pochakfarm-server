package com.somagochi.pochakfarm.user.infrastructure.persistence;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT user FROM User user WHERE user.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);

  Optional<User> findBySocialAccountProviderAndEmail(SocialProvider provider, String email);

  Optional<User> findUserByNickname(String nickname);
}
