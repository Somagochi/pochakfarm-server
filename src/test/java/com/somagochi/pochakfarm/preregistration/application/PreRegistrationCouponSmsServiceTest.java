package com.somagochi.pochakfarm.preregistration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.common.notification.BulkSmsNotification;
import com.somagochi.pochakfarm.common.notification.Notification;
import com.somagochi.pochakfarm.common.notification.NotificationResult;
import com.somagochi.pochakfarm.common.notification.NotificationService;
import com.somagochi.pochakfarm.coupon.application.CouponQueryService;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistrationStatus;
import com.somagochi.pochakfarm.preregistration.dto.PreRegistrationCouponSmsResult;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class PreRegistrationCouponSmsServiceTest {

  private PreRegistrationRepository preRegistrationRepository;
  private PreRegistrationCryptoService cryptoService;
  private CouponQueryService couponQueryService;
  private NotificationService notificationService;
  private PreRegistrationCouponSmsService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    preRegistrationRepository = mock(PreRegistrationRepository.class);
    cryptoService = mock(PreRegistrationCryptoService.class);
    couponQueryService = mock(CouponQueryService.class);
    notificationService = mock(NotificationService.class);
    TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    doAnswer(
            invocation -> {
              ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());
    service =
        new PreRegistrationCouponSmsService(
            preRegistrationRepository,
            cryptoService,
            couponQueryService,
            notificationService,
            transactionTemplate);
  }

  @Test
  void sendsCouponSmsAndMarksSent() {
    PreRegistration first = preRegistration(1L, "enc-1");
    PreRegistration second = preRegistration(2L, "enc-2");
    given(
            preRegistrationRepository.findAllByStatusAndMessageSentAtIsNull(
                PreRegistrationStatus.REGISTERED))
        .willReturn(List.of(first, second));
    given(couponQueryService.findCouponCodesByPreRegistrationIds(List.of(1L, 2L)))
        .willReturn(Map.of(1L, "AAAAAA", 2L, "BBBBBB"));
    given(cryptoService.decrypt("enc-1")).willReturn("01011112222");
    given(cryptoService.decrypt("enc-2")).willReturn("01033334444");
    given(notificationService.notify(any(Notification.class)))
        .willReturn(NotificationResult.success());
    given(preRegistrationRepository.findAllById(List.of(1L, 2L)))
        .willReturn(List.of(first, second));

    PreRegistrationCouponSmsResult result = service.send(false);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationService).notify(captor.capture());
    BulkSmsNotification bulk = (BulkSmsNotification) captor.getValue();
    assertEquals(2, bulk.messages().size());
    assertEquals("01011112222", bulk.messages().get(0).to());
    assertEquals("[포착팜] 사전등록 쿠폰이 도착했어요!\n쿠폰 코드: AAAAAA", bulk.messages().get(0).text());
    assertEquals(new PreRegistrationCouponSmsResult(2, 2, 0, 0, false), result);
    assertEquals(true, first.getMessageSentAt() != null);
    assertEquals(true, second.getMessageSentAt() != null);
  }

  @Test
  void excludesFailedRecipientsFromMarking() {
    PreRegistration first = preRegistration(1L, "enc-1");
    PreRegistration second = preRegistration(2L, "enc-2");
    given(
            preRegistrationRepository.findAllByStatusAndMessageSentAtIsNull(
                PreRegistrationStatus.REGISTERED))
        .willReturn(List.of(first, second));
    given(couponQueryService.findCouponCodesByPreRegistrationIds(List.of(1L, 2L)))
        .willReturn(Map.of(1L, "AAAAAA", 2L, "BBBBBB"));
    given(cryptoService.decrypt("enc-1")).willReturn("01011112222");
    given(cryptoService.decrypt("enc-2")).willReturn("01033334444");
    given(notificationService.notify(any(Notification.class)))
        .willReturn(new NotificationResult(List.of("01033334444")));
    given(preRegistrationRepository.findAllById(List.of(1L))).willReturn(List.of(first));

    PreRegistrationCouponSmsResult result = service.send(false);

    assertEquals(new PreRegistrationCouponSmsResult(2, 1, 1, 0, false), result);
    verify(preRegistrationRepository).findAllById(List.of(1L));
  }

  @Test
  void skipsTargetsWithoutIssuedCoupon() {
    PreRegistration first = preRegistration(1L, "enc-1");
    PreRegistration second = preRegistration(2L, "enc-2");
    given(
            preRegistrationRepository.findAllByStatusAndMessageSentAtIsNull(
                PreRegistrationStatus.REGISTERED))
        .willReturn(List.of(first, second));
    given(couponQueryService.findCouponCodesByPreRegistrationIds(List.of(1L, 2L)))
        .willReturn(Map.of(1L, "AAAAAA"));
    given(cryptoService.decrypt("enc-1")).willReturn("01011112222");
    given(notificationService.notify(any(Notification.class)))
        .willReturn(NotificationResult.success());
    given(preRegistrationRepository.findAllById(List.of(1L))).willReturn(List.of(first));

    PreRegistrationCouponSmsResult result = service.send(false);

    assertEquals(new PreRegistrationCouponSmsResult(2, 1, 0, 1, false), result);
  }

  @Test
  void dryRunCountsWithoutSending() {
    PreRegistration first = preRegistration(1L, "enc-1");
    given(
            preRegistrationRepository.findAllByStatusAndMessageSentAtIsNull(
                PreRegistrationStatus.REGISTERED))
        .willReturn(List.of(first));
    given(couponQueryService.findCouponCodesByPreRegistrationIds(List.of(1L)))
        .willReturn(Map.of(1L, "AAAAAA"));

    PreRegistrationCouponSmsResult result = service.send(true);

    assertEquals(new PreRegistrationCouponSmsResult(1, 0, 0, 0, true), result);
    verify(notificationService, never()).notify(any(Notification.class));
  }

  private PreRegistration preRegistration(Long id, String phoneNumberEncrypted) {
    PreRegistration preRegistration = mock(PreRegistration.class);
    given(preRegistration.getId()).willReturn(id);
    given(preRegistration.getPhoneNumberEncrypted()).willReturn(phoneNumberEncrypted);
    Instant[] sentAt = new Instant[1];
    doAnswer(
            invocation -> {
              sentAt[0] = invocation.getArgument(0);
              return null;
            })
        .when(preRegistration)
        .markCouponSmsSent(any(Instant.class));
    given(preRegistration.getMessageSentAt()).willAnswer(invocation -> sentAt[0]);
    return preRegistration;
  }
}
