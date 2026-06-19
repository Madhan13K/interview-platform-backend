package com.interview_platform_backend.interview_platform_backend.dei.repository;

import com.interview_platform_backend.interview_platform_backend.dei.entity.DemographicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemographicProfileRepository extends JpaRepository<DemographicProfile, UUID> {

    Optional<DemographicProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<DemographicProfile> findByConsentGivenTrue();

    long countByConsentGivenTrue();

    @Query("SELECT dp.gender, COUNT(dp) FROM DemographicProfile dp WHERE dp.consentGiven = true AND dp.gender IS NOT NULL GROUP BY dp.gender")
    List<Object[]> countByGender();

    @Query("SELECT dp.ethnicity, COUNT(dp) FROM DemographicProfile dp WHERE dp.consentGiven = true AND dp.ethnicity IS NOT NULL GROUP BY dp.ethnicity")
    List<Object[]> countByEthnicity();

    @Query("SELECT dp.ageRange, COUNT(dp) FROM DemographicProfile dp WHERE dp.consentGiven = true AND dp.ageRange IS NOT NULL GROUP BY dp.ageRange")
    List<Object[]> countByAgeRange();

    @Query("SELECT dp.veteranStatus, COUNT(dp) FROM DemographicProfile dp WHERE dp.consentGiven = true AND dp.veteranStatus IS NOT NULL GROUP BY dp.veteranStatus")
    List<Object[]> countByVeteranStatus();

    @Query("SELECT dp.disabilityStatus, COUNT(dp) FROM DemographicProfile dp WHERE dp.consentGiven = true AND dp.disabilityStatus IS NOT NULL GROUP BY dp.disabilityStatus")
    List<Object[]> countByDisabilityStatus();

    @Query("SELECT dp FROM DemographicProfile dp JOIN dp.user u JOIN com.interview_platform_backend.interview_platform_backend.jobboard.entity.JobApplication ja ON ja.candidate.id = u.id WHERE ja.status = :status AND dp.consentGiven = true")
    List<DemographicProfile> findByApplicationStatus(@org.springframework.data.repository.query.Param("status") com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationStatus status);
}
