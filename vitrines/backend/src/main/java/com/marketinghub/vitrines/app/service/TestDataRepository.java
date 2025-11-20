package com.marketinghub.vitrines.app.service;

import com.marketinghub.vitrines.app.model.Role;
import com.marketinghub.vitrines.app.model.TestUser;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TestDataRepository {

  private final Map<String, TestUser> users = new ConcurrentHashMap<>();

  public TestDataRepository() {
    save(new TestUser("cliente@vitrineproduto.shop", Role.CLIENTE, "plan_premium_ads", true));
    save(new TestUser("lead@vitrineproduto.shop", Role.LEAD, null, true));
  }

  public Collection<TestUser> findAll() {
    return users.values();
  }

  public TestUser ensureUser(String email, Role role, String planId, boolean firstAccess) {
    String normalizedEmail = normalizeEmail(email);
    TestUser existing = users.get(normalizedEmail);

    if (existing == null) {
      TestUser user = new TestUser(normalizedEmail, role, planId, firstAccess);
      return save(user);
    }

    TestUser merged =
        new TestUser(
            existing.email(),
            role != null ? role : existing.role(),
            planId != null && !planId.isBlank() ? planId : existing.planId(),
            existing.firstAccess() || firstAccess);

    return save(merged);
  }

  private TestUser save(TestUser user) {
    users.put(normalizeEmail(user.email()), user);
    return user;
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
