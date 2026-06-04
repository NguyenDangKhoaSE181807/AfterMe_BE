package com.example.reminder.service.impl;

import com.example.reminder.dto.userdevice.UpdateUserDeviceNotificationRequest;
import com.example.reminder.dto.userdevice.UpsertUserDeviceRequest;
import com.example.reminder.dto.userdevice.UserDeviceResponseDto;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserDevice;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.UserDeviceRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.UserDeviceService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeviceServiceImpl implements UserDeviceService {

	private final UserRepository userRepository;
	private final UserDeviceRepository userDeviceRepository;

	@Override
	@Transactional
	public UserDeviceResponseDto upsertCurrentDevice(String userEmail, String deviceId, UpsertUserDeviceRequest request) {
		User user = getActiveUser(userEmail);
		UserDevice device = userDeviceRepository.findByUser_IdAndDeviceId(user.getId(), deviceId)
				.orElseGet(UserDevice::new);

		device.setUser(user);
		device.setDeviceId(deviceId);
		device.setFcmToken(request.fcmToken().trim());
		device.setPlatform(request.platform() == null || request.platform().isBlank() ? null : request.platform().trim());
		device.setNotificationEnabled(request.notificationEnabled() == null ? Boolean.TRUE : request.notificationEnabled());
		device.setIsTrusted(request.isTrusted() == null ? Boolean.FALSE : request.isTrusted());
		device.setLastSeenAt(LocalDateTime.now());

		return toDto(userDeviceRepository.save(device));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserDeviceResponseDto> listMyDevices(String userEmail) {
		User user = getActiveUser(userEmail);
		return userDeviceRepository.findByUser_Id(user.getId())
				.stream()
				.map(this::toDto)
				.toList();
	}

	@Override
	@Transactional
	public UserDeviceResponseDto updateCurrentDeviceNotificationEnabled(
			String userEmail,
			String deviceId,
			UpdateUserDeviceNotificationRequest request
	) {
		UserDevice device = findOwnedDevice(userEmail, deviceId);
		device.setNotificationEnabled(request.notificationEnabled());
		device.setLastSeenAt(LocalDateTime.now());
		return toDto(userDeviceRepository.save(device));
	}

	@Override
	@Transactional
	public void deleteCurrentDevice(String userEmail, String deviceId) {
		UserDevice device = findOwnedDevice(userEmail, deviceId);
		userDeviceRepository.delete(device);
	}

	private UserDevice findOwnedDevice(String userEmail, String deviceId) {
		User user = getActiveUser(userEmail);
		return userDeviceRepository.findByUser_IdAndDeviceId(user.getId(), deviceId)
				.orElseThrow(() -> new ResourceNotFoundException("User device not found: " + deviceId));
	}

	private User getActiveUser(String email) {
		return userRepository.findByEmailAndDeletedAtIsNull(email)
				.orElseThrow(() -> new BadRequestException("User not found"));
	}

	private UserDeviceResponseDto toDto(UserDevice device) {
		return new UserDeviceResponseDto(
				device.getId(),
				device.getDeviceId(),
				device.getFcmToken(),
				device.getPlatform(),
				device.getIsTrusted(),
				device.getNotificationEnabled(),
				device.getLastSeenAt()
		);
	}
}