package com.marketinghub.repository.jpa.settings;

import com.marketinghub.settings.GeneralSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de GeneralSetting.
 */
public interface GeneralSettingRepository extends JpaRepository<GeneralSetting, Long> {
    Optional<GeneralSetting> findByName(String name);
}
