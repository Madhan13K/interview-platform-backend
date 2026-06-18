package com.interview_platform_backend.interview_platform_backend.gdpr.repository;

import com.interview_platform_backend.interview_platform_backend.gdpr.entity.DataErasureRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataErasureRequestRepository extends JpaRepository<DataErasureRequest, UUID> {

    List<DataErasureRequest> findByUserId(UUID userId);

    List<DataErasureRequest> findByStatus(DataErasureRequest.ErasureStatus status);
}
