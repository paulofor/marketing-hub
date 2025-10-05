package com.marketinghub.ads;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/instagram")
public class InstagramAccountController {
    private final InstagramAccountRepository repository;

    public InstagramAccountController(InstagramAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<InstagramAccount> findAll() {
        return repository.findAll();
    }

    @PostMapping
    public InstagramAccount create(@RequestBody InstagramAccount account) {
        validateRequiredFields(account);
        account.setCurrency("BRL");
        return repository.save(account);
    }

    @PutMapping("/{id}")
    public InstagramAccount update(@PathVariable Long id, @RequestBody InstagramAccount account) {
        InstagramAccount existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        validateRequiredFields(account);

        existing.setName(account.getName());
        existing.setAvatarUrl(account.getAvatarUrl());
        existing.setInstagramUserId(account.getInstagramUserId());
        existing.setFacebookPageId(account.getFacebookPageId());
        existing.setAdAccountId(account.getAdAccountId());
        existing.setCurrency("BRL");

        if (account.isAccessTokenProvided()) {
            existing.setAccessToken(account.getAccessToken());
        }

        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private void validateRequiredFields(InstagramAccount account) {
        if (!StringUtils.hasText(account.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome é obrigatório");
        }
        if (!StringUtils.hasText(account.getInstagramUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Instagram user ID é obrigatório");
        }
        if (!StringUtils.hasText(account.getAdAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ad account ID é obrigatório");
        }
        if (!StringUtils.hasText(account.getFacebookPageId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Facebook Page ID é obrigatório");
        }
    }
}
