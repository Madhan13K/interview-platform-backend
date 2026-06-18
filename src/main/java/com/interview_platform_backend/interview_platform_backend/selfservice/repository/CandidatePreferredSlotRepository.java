package com.interview_platform_backend.interview_platform_backend.selfservice.repository;

import com.interview_platform_backend.interview_platform_backend.selfservice.entity.CandidatePreferredSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidatePreferredSlotRepository extends JpaRepository<CandidatePreferredSlot, UUID> {

    List<CandidatePreferredSlot> findByCandidateIdOrderByPriorityAsc(UUID candidateId);

    List<CandidatePreferredSlot> findByInterviewId(UUID interviewId);

    List<CandidatePreferredSlot> findByJobPositionId(UUID jobPositionId);

    List<CandidatePreferredSlot> findByCandidateIdAndStatus(UUID candidateId, CandidatePreferredSlot.SlotStatus status);
}

