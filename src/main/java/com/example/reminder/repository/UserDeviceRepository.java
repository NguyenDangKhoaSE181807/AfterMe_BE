package com.example.reminder.repository;

import com.example.reminder.entity.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUser_IdAndDeviceId(Long userId, String deviceId);

    List<UserDevice> findByUser_Id(Long userId);

    List<UserDevice> findByUser_IdAndNotificationEnabledTrueAndFcmTokenIsNotNull(Long userId);

    Optional<UserDevice> findFirstByUser_IdAndLastLatitudeIsNotNullAndLastLongitudeIsNotNullAndLastLocationAtIsNotNullOrderByLastLocationAtDescLastSeenAtDesc(Long userId);
}
