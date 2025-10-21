package com.marketinghub.worker.settings;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GeneralSettingPrivacyPolicyProvider implements PrivacyPolicyProvider {

    static final String PRIVACY_POLICY_KEY = "privacy_policy_url";

    private final GeneralSettingValueRepository repository;

    public GeneralSettingPrivacyPolicyProvider(GeneralSettingValueRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> getPrivacyPolicyUrl() {
        return repository.findValue(PRIVACY_POLICY_KEY);
    }
}
