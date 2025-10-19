package com.marketinghub.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeneralSettingRepository extends JpaRepository<GeneralSetting, Long> {
    Optional<GeneralSetting> findByName(String name);
}
