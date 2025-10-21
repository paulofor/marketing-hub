package com.marketinghub.worker.settings;

import java.util.Optional;

/**
 * Provides access to the configured privacy policy URL used when
 * generating instant forms for experiments.
 */
public interface PrivacyPolicyProvider {
    Optional<String> getPrivacyPolicyUrl();
}
