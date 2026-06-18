package com.interview_platform_backend.interview_platform_backend.accountlockout.repository;

import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.IpBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpBlocklistRepository extends JpaRepository<IpBlocklist, UUID> {

    Optional<IpBlocklist> findByIpAddressAndActiveTrue(String ipAddress);

    boolean existsByIpAddressAndActiveTrue(String ipAddress);

    List<IpBlocklist> findByActiveTrue();

    @Query("SELECT ib FROM IpBlocklist ib WHERE ib.active = true AND ib.expiresAt IS NOT NULL AND ib.expiresAt < :now")
    List<IpBlocklist> findExpiredBlocks(@Param("now") Instant now);
}
