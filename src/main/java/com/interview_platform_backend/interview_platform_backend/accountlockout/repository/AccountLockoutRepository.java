package com.interview_platform_backend.interview_platform_backend.accountlockout.repository;

import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.AccountLockout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountLockoutRepository extends JpaRepository<AccountLockout, UUID> {

    Optional<AccountLockout> findByEmail(String email);

    boolean existsByEmailAndLockedTrue(String email);
}
