package com.example.reminder.repository;

import com.example.reminder.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<User> findAllByDeletedAtIsNull();

    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}





