package com.example.reminder.repository;

import com.example.reminder.entity.FamilyMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findBySubscriptionId(Long subscriptionId);
}
