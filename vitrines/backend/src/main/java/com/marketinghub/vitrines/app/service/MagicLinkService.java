package com.marketinghub.vitrines.app.service;

import com.marketinghub.vitrines.app.config.MagicLinkProperties;
import com.marketinghub.vitrines.app.model.MagicLinkRequest;
import com.marketinghub.vitrines.app.model.MagicLinkResponse;
import com.marketinghub.vitrines.app.model.Role;
import com.marketinghub.vitrines.app.model.TestUser;
import io.jsonwebtoken.Jwts;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MagicLinkService {

  private final MagicLinkProperties properties;
  private final TestDataRepository testDataRepository;

  public MagicLinkService(MagicLinkProperties properties, TestDataRepository testDataRepository) {
    this.properties = properties;
    this.testDataRepository = testDataRepository;
  }

  public MagicLinkResponse generate(MagicLinkRequest request) {
    MagicLinkRequest safeRequest = Objects.requireNonNullElseGet(request, () -> new MagicLinkRequest(null, null, null, null));

    String email = resolveEmail(safeRequest.email());
    Role role = Role.from(StringUtils.hasText(safeRequest.role()) ? safeRequest.role() : Role.CLIENTE.name());
    String planId = StringUtils.hasText(safeRequest.planId()) ? safeRequest.planId() : "plan_premium_ads";
    boolean firstAccess = safeRequest.firstAccess() == null || safeRequest.firstAccess();

    TestUser user = testDataRepository.ensureUser(email, role, planId, firstAccess);

    Instant now = Instant.now();
    Instant expiresAt = now.plus(properties.ttl());

    String token =
        Jwts.builder()
            .setSubject(user.email())
            .setIssuer(properties.issuer())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .claim("email", user.email())
            .claim("role", user.role().name())
            .claim("planId", user.planId())
            .claim("first_access", user.firstAccess())
            .claim("domain", properties.domain())
            .signWith(properties.signingKey())
            .compact();

    String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
    String link = properties.domain() + "/auth/magic?token=" + encodedToken;

    return new MagicLinkResponse(user.email(), user.role().name(), user.planId(), user.firstAccess(), token, link, expiresAt);
  }

  private String resolveEmail(String requestedEmail) {
    if (StringUtils.hasText(requestedEmail)) {
      return requestedEmail.trim().toLowerCase();
    }

    return "cliente@vitrineproduto.shop";
  }
}
