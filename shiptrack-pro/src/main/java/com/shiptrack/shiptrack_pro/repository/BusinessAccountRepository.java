package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.BusinessAccount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessAccountRepository
        extends JpaRepository<BusinessAccount, Long> {

    Optional<BusinessAccount> findByCreatedBy(
            String createdBy
    );
}