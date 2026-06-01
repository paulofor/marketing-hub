package com.marketinghub.whatsapp.service;

import com.marketinghub.whatsapp.WhatsAppAccount;
import com.marketinghub.whatsapp.dto.WhatsAppAccountRequest;
import com.marketinghub.repository.jpa.whatsapp.WhatsAppAccountRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Service encapsulating persistence logic for WhatsApp accounts.
 */
@Service
public class WhatsAppAccountService {
    private final WhatsAppAccountRepository repository;

    public WhatsAppAccountService(WhatsAppAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<WhatsAppAccount> listAccounts() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional
    public WhatsAppAccount createAccount(WhatsAppAccountRequest request) {
        WhatsAppAccount account = new WhatsAppAccount();
        apply(request, account);
        if (Boolean.TRUE.equals(request.getActive())) {
            repository.deactivateAll();
            account.setActive(true);
        }
        return repository.save(account);
    }

    @Transactional
    public WhatsAppAccount updateAccount(Long id, WhatsAppAccountRequest request) {
        WhatsAppAccount account = repository.findById(id).orElseThrow();
        apply(request, account);
        if (request.getActive() != null) {
            if (request.getActive()) {
                repository.deactivateAllExcept(account.getId());
                account.setActive(true);
            } else {
                account.setActive(false);
            }
        }
        return repository.save(account);
    }

    @Transactional(readOnly = true)
    public Optional<WhatsAppAccount> findActiveAccount() {
        return repository.findFirstByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<WhatsAppAccount> findByPhoneNumberId(String phoneNumberId) {
        if (!StringUtils.hasText(phoneNumberId)) {
            return Optional.empty();
        }
        return repository.findByPhoneNumberId(phoneNumberId);
    }

    @Transactional(readOnly = true)
    public Optional<WhatsAppAccount> findByVerifyToken(String verifyToken) {
        if (!StringUtils.hasText(verifyToken)) {
            return Optional.empty();
        }
        return repository.findByVerifyToken(verifyToken);
    }

    @Transactional(readOnly = true)
    public WhatsAppAccount requireActiveAccount() {
        return findActiveAccount().orElseThrow(() -> new IllegalStateException("No active WhatsApp account configured"));
    }

    private void apply(WhatsAppAccountRequest request, WhatsAppAccount account) {
        account.setDisplayName(request.getDisplayName());
        account.setPhoneNumber(request.getPhoneNumber());
        account.setPhoneNumberId(request.getPhoneNumberId());
        account.setBusinessAccountId(request.getBusinessAccountId());
        account.setAccessToken(request.getAccessToken());
        account.setVerifyToken(request.getVerifyToken());
        account.setBaseUrl(request.getBaseUrl());
    }
}
