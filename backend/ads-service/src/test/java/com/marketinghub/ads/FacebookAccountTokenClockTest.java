package com.marketinghub.ads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/** Impede que fusos diferentes no backend e no worker atrasem a renovação Meta. */
@ResourceLock("java.util.TimeZone.default")
class FacebookAccountTokenClockTest {
  /** Compara a validade oficial em UTC mesmo quando o host usa o horário de Brasília. */
  @Test
  void expirationAndRenewalWindowUseUtc() {
    TimeZone previous = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
      FacebookAccount account =
          FacebookAccount.builder()
              .accessToken("synthetic")
              .tokenExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30))
              .build();
      assertThat(account.isTokenExpired()).isTrue();
      account.setTokenExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7).minusHours(2));
      assertThat(account.isTokenRenewalRequired()).isTrue();
      account.setTokenExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(8));
      assertThat(account.isTokenRenewalRequired()).isFalse();
    } finally {
      TimeZone.setDefault(previous);
    }
  }
}
