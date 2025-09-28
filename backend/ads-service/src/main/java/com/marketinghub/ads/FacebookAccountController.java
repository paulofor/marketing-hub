package com.marketinghub.ads;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/accounts/facebook")
public class FacebookAccountController {
    private final FacebookAccountRepository repository;

    public FacebookAccountController(FacebookAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<FacebookAccount> findAll() {
        return repository.findAll();
    }

    @PostMapping
    public FacebookAccount create(@RequestBody FacebookAccount account) {
        normalizeAccount(account);
        if (account.getAccessToken() == null) {
            account.setTokenExpiresAt(null);
            account.setTokenLastRefreshedAt(null);
        } else if (account.getTokenLastRefreshedAt() == null) {
            account.setTokenLastRefreshedAt(LocalDateTime.now());
        }
        return repository.save(account);
    }

    @PutMapping("/{id}")
    public FacebookAccount update(@PathVariable Long id, @RequestBody FacebookAccount account) {
        FacebookAccount persisted = repository.findById(id).orElseThrow();
        normalizeAccount(account);

        persisted.setName(account.getName());
        persisted.setCurrency(account.getCurrency());
        persisted.setAuthorizedUserId(account.getAuthorizedUserId());
        persisted.setAuthorizedUserName(account.getAuthorizedUserName());
        persisted.setAuthorizedUserEmail(account.getAuthorizedUserEmail());

        String newToken = account.getAccessToken();
        if (newToken == null) {
            persisted.setAccessToken(null);
            persisted.setTokenExpiresAt(null);
            persisted.setTokenLastRefreshedAt(null);
        } else {
            if (!newToken.equals(persisted.getAccessToken())) {
                persisted.setTokenLastRefreshedAt(LocalDateTime.now());
            }
            persisted.setAccessToken(newToken);
            persisted.setTokenExpiresAt(account.getTokenExpiresAt());
        }

        return repository.save(persisted);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private void normalizeAccount(FacebookAccount account) {
        account.setName(trim(account.getName()));
        account.setCurrency(trim(account.getCurrency()));
        account.setAccessToken(trimToNull(account.getAccessToken()));
        account.setAuthorizedUserId(trimToNull(account.getAuthorizedUserId()));
        account.setAuthorizedUserName(trimToNull(account.getAuthorizedUserName()));
        account.setAuthorizedUserEmail(trimToNull(account.getAuthorizedUserEmail()));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
