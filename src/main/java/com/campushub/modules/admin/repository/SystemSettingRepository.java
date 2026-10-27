package com.campushub.modules.admin.repository;

import com.campushub.modules.admin.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
