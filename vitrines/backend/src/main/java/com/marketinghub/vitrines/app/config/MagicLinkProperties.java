package com.marketinghub.vitrines.app.config;

import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "magic-link")
public class MagicLinkProperties {

  private final String domain;
  private final String issuer;
  private final Duration ttl;
  private final String secret;

  public MagicLinkProperties(String domain, String issuer, Duration ttl, String secret) {
    this.domain = require(domain, "domain");
    this.issuer = require(issuer, "issuer");
    this.ttl = Objects.requireNonNullElse(ttl, Duration.ofMinutes(30));
    this.secret = require(secret, "secret");
  }

  public String domain() {
    return domain;
  }

  public String issuer() {
    return issuer;
  }

  public Duration ttl() {
    return ttl;
  }

  public Key signingKey() {
    return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
  }

  private String require(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("magic-link." + fieldName + " deve ser preenchido");
    }

    return value;
  }
}
