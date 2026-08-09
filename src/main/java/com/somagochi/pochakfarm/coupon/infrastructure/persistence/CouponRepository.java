package com.somagochi.pochakfarm.coupon.infrastructure.persistence;

import com.somagochi.pochakfarm.coupon.domain.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Coupon c where c.couponCode = :couponCode")
  Optional<Coupon> findByCouponCodeForUpdate(@Param("couponCode") String couponCode);

  boolean existsByCouponCode(String couponCode);
}
