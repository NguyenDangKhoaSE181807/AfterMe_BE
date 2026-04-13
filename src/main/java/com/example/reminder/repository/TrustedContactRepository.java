package com.example.reminder.repository;

import com.example.reminder.entity.TrustedContact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedContactRepository extends JpaRepository<TrustedContact, Long> {

    List<TrustedContact> findByUserIdAndDeletedAtIsNull(Long userId);
}





