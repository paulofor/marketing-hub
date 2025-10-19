package com.marketinghub.settings;

import com.marketinghub.settings.dto.GeneralSettingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class GeneralSettingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralSettingService.class);

    private final GeneralSettingRepository repository;

    public GeneralSettingService(GeneralSettingRepository repository) {
        this.repository = repository;
    }

    public Optional<GeneralSettingDto> findByName(String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        String normalized = normalizeName(name);
        return repository.findByName(normalized).map(this::toDto);
    }

    public Optional<String> findValue(String name) {
        return findByName(name).map(GeneralSettingDto::value).map(this::sanitizeValue);
    }

    public Optional<String> getPrivacyPolicyUrl() {
        return findValue(GeneralSettingKeys.PRIVACY_POLICY_URL);
    }

    @Transactional
    public GeneralSettingDto upsert(String name, String value) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name is required");
        }
        String normalizedName = normalizeName(name);
        GeneralSetting entity = repository.findByName(normalizedName)
                .orElseGet(() -> GeneralSetting.builder().name(normalizedName).build());
        entity.setValue(sanitizeValue(value));
        GeneralSetting saved = repository.save(entity);
        LOGGER.info("Updated general setting {}", normalizedName);
        return toDto(saved);
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private GeneralSettingDto toDto(GeneralSetting entity) {
        return new GeneralSettingDto(entity.getName(), entity.getValue(), entity.getUpdatedAt());
    }
}
