package com.example.reminder.service.impl;

import com.example.reminder.entity.DigitalAsset;
import com.example.reminder.repository.DigitalAssetRepository;
import com.example.reminder.service.DigitalAssetService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DigitalAssetServiceImpl implements DigitalAssetService {

    private final DigitalAssetRepository digitalAssetRepository;

    @Override
    public List<DigitalAsset> findByUserId(Long userId) {
        return digitalAssetRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Optional<DigitalAsset> findById(Long id) {
        return digitalAssetRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public DigitalAsset save(DigitalAsset asset) {
        return digitalAssetRepository.save(asset);
    }
}
