package com.marketinghub.worker.settings;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
public class GeneralSettingValueRepository {

    private static final String SQL_FIND_BY_NAME =
            "select setting_value from general_setting where name = :name limit 1";

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Optional<String> findValue(String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        String normalized = normalizeName(name);
        Query query = entityManager.createNativeQuery(SQL_FIND_BY_NAME);
        query.setParameter("name", normalized);
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.stream()
                .map(this::convertValue)
                .map(this::sanitize)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String convertValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return Objects.toString(value, null);
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
