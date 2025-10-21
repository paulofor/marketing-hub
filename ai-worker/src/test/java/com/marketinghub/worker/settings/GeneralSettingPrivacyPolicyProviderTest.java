package com.marketinghub.worker.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralSettingPrivacyPolicyProviderTest {

    @Mock
    private GeneralSettingValueRepository repository;

    private GeneralSettingPrivacyPolicyProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GeneralSettingPrivacyPolicyProvider(repository);
    }

    @Test
    void shouldReturnConfiguredPrivacyPolicyUrl() {
        when(repository.findValue("privacy_policy_url")).thenReturn(Optional.of("https://example.com/privacy"));

        Optional<String> result = provider.getPrivacyPolicyUrl();

        assertThat(result).contains("https://example.com/privacy");
        verify(repository).findValue("privacy_policy_url");
    }

    @Test
    void shouldReturnEmptyWhenValueIsMissing() {
        when(repository.findValue("privacy_policy_url")).thenReturn(Optional.empty());

        Optional<String> result = provider.getPrivacyPolicyUrl();

        assertThat(result).isEmpty();
        verify(repository).findValue("privacy_policy_url");
    }
}
