package com.interview_platform_backend.interview_platform_backend.dei.service;

import com.interview_platform_backend.interview_platform_backend.dei.dto.*;
import com.interview_platform_backend.interview_platform_backend.dei.entity.DemographicProfile;
import com.interview_platform_backend.interview_platform_backend.dei.repository.DemographicProfileRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationStatus;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeiService {

    private static final Logger log = LoggerFactory.getLogger(DeiService.class);

    private final DemographicProfileRepository demographicProfileRepository;
    private final UserRepository userRepository;

    public DeiService(DemographicProfileRepository demographicProfileRepository,
                      UserRepository userRepository) {
        this.demographicProfileRepository = demographicProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DemographicProfileResponse submitDemographics(DemographicProfileRequest request, String userEmail) {
        if (!Boolean.TRUE.equals(request.getConsentGiven())) {
            throw new BadRequestException("Consent must be given to store demographic data");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Optional<DemographicProfile> existing = demographicProfileRepository.findByUserId(user.getId());

        DemographicProfile profile;
        if (existing.isPresent()) {
            profile = existing.get();
            profile.setGender(request.getGender());
            profile.setEthnicity(request.getEthnicity());
            profile.setVeteranStatus(request.getVeteranStatus());
            profile.setDisabilityStatus(request.getDisabilityStatus());
            profile.setAgeRange(request.getAgeRange());
            profile.setConsentGiven(true);
            if (!existing.get().isConsentGiven()) {
                profile.setConsentGivenAt(Instant.now());
            }
        } else {
            profile = DemographicProfile.builder()
                    .user(user)
                    .gender(request.getGender())
                    .ethnicity(request.getEthnicity())
                    .veteranStatus(request.getVeteranStatus())
                    .disabilityStatus(request.getDisabilityStatus())
                    .ageRange(request.getAgeRange())
                    .consentGiven(true)
                    .consentGivenAt(Instant.now())
                    .build();
        }

        DemographicProfile saved = demographicProfileRepository.save(profile);
        log.info("Demographic profile submitted/updated for user {}", userEmail);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public DemographicProfileResponse getMyDemographics(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        DemographicProfile profile = demographicProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DemographicProfile", "userId", user.getId()));

        return mapToResponse(profile);
    }

    @Transactional
    public void revokeDemographics(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        DemographicProfile profile = demographicProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DemographicProfile", "userId", user.getId()));

        demographicProfileRepository.delete(profile);
        log.info("Demographic profile revoked/deleted for user {}", userEmail);
    }

    @Transactional(readOnly = true)
    public DiversityDashboardResponse getDiversityDashboard() {
        long totalProfiles = demographicProfileRepository.count();
        long totalWithConsent = demographicProfileRepository.countByConsentGivenTrue();

        Map<String, Integer> genderDistribution = aggregateResults(demographicProfileRepository.countByGender());
        Map<String, Integer> ethnicityDistribution = aggregateResults(demographicProfileRepository.countByEthnicity());
        Map<String, Integer> ageRangeDistribution = aggregateResults(demographicProfileRepository.countByAgeRange());
        Map<String, Integer> veteranDistribution = aggregateBooleanResults(demographicProfileRepository.countByVeteranStatus());
        Map<String, Integer> disabilityDistribution = aggregateBooleanResults(demographicProfileRepository.countByDisabilityStatus());

        // Pipeline breakdown by application status
        List<DiversityFunnelResponse> pipelineBreakdown = buildPipelineBreakdown();

        return DiversityDashboardResponse.builder()
                .totalProfiles(totalProfiles)
                .totalWithConsent(totalWithConsent)
                .genderDistribution(genderDistribution)
                .ethnicityDistribution(ethnicityDistribution)
                .ageRangeDistribution(ageRangeDistribution)
                .veteranStatusDistribution(veteranDistribution)
                .disabilityStatusDistribution(disabilityDistribution)
                .pipelineBreakdown(pipelineBreakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DiversityFunnelResponse> getFunnelAnalysis() {
        return buildPipelineBreakdown();
    }

    private List<DiversityFunnelResponse> buildPipelineBreakdown() {
        List<DiversityFunnelResponse> funnel = new ArrayList<>();

        for (ApplicationStatus status : ApplicationStatus.values()) {
            List<DemographicProfile> profiles = demographicProfileRepository.findByApplicationStatus(status);
            if (profiles.isEmpty()) continue;

            Map<String, Integer> genderDist = new LinkedHashMap<>();
            Map<String, Integer> ethnicityDist = new LinkedHashMap<>();
            Map<String, Integer> ageRangeDist = new LinkedHashMap<>();

            for (DemographicProfile dp : profiles) {
                if (dp.getGender() != null) {
                    genderDist.merge(dp.getGender().name(), 1, Integer::sum);
                }
                if (dp.getEthnicity() != null) {
                    ethnicityDist.merge(dp.getEthnicity().name(), 1, Integer::sum);
                }
                if (dp.getAgeRange() != null) {
                    ageRangeDist.merge(dp.getAgeRange().name(), 1, Integer::sum);
                }
            }

            funnel.add(DiversityFunnelResponse.builder()
                    .stage(status.name())
                    .totalCandidates(profiles.size())
                    .genderDistribution(genderDist)
                    .ethnicityDistribution(ethnicityDist)
                    .ageRangeDistribution(ageRangeDist)
                    .build());
        }

        return funnel;
    }

    private Map<String, Integer> aggregateResults(List<Object[]> results) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (Object[] row : results) {
            String key = row[0] != null ? row[0].toString() : "UNKNOWN";
            int count = ((Number) row[1]).intValue();
            distribution.put(key, count);
        }
        return distribution;
    }

    private Map<String, Integer> aggregateBooleanResults(List<Object[]> results) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (Object[] row : results) {
            String key = row[0] != null ? (Boolean.TRUE.equals(row[0]) ? "YES" : "NO") : "NOT_SPECIFIED";
            int count = ((Number) row[1]).intValue();
            distribution.put(key, count);
        }
        return distribution;
    }

    private DemographicProfileResponse mapToResponse(DemographicProfile profile) {
        return DemographicProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .gender(profile.getGender())
                .ethnicity(profile.getEthnicity())
                .veteranStatus(profile.getVeteranStatus())
                .disabilityStatus(profile.getDisabilityStatus())
                .ageRange(profile.getAgeRange())
                .consentGiven(profile.isConsentGiven())
                .consentGivenAt(profile.getConsentGivenAt())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
