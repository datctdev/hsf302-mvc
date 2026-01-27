package com.hsf.e_comerce.platform.repository;

import com.hsf.e_comerce.platform.entity.PlatformSetting;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, UUID> {

    Optional<PlatformSetting> findByKey(String key);

    boolean existsByKey(String key);
}
