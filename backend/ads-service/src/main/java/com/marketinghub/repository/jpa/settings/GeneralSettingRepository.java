package com.marketinghub.repository.jpa.settings;

import com.marketinghub.settings.GeneralSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de GeneralSetting. */
public interface GeneralSettingRepository extends JpaRepository<GeneralSetting, Long> {
  Optional<GeneralSetting> findByName(String name);
}
