package com.example.reminder.repository;

import com.example.reminder.entity.AssetAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetAccessLogRepository extends JpaRepository<AssetAccessLog, Long> {
}