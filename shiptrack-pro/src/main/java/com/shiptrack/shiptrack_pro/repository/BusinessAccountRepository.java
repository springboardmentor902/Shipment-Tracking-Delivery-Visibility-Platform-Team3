package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.BusinessAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessAccountRepository extends JpaRepository<BusinessAccount, Long> {
    Optional<BusinessAccount> findByOwnerId(Long ownerId);
    boolean existsByCompanyName(String companyName);
}
