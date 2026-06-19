package com.interview_platform_backend.interview_platform_backend.sourcetracking.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobboard.entity.JobApplication;
import com.interview_platform_backend.interview_platform_backend.jobboard.repository.JobApplicationRepository;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.dto.SourceDashboardResponse;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.dto.SourceEffectivenessResponse;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.dto.TrackSourceRequest;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.CandidateSource;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.SourceType;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.repository.CandidateSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SourceTrackingService {

    private static final Logger log = LoggerFactory.getLogger(SourceTrackingService.class);

    private final CandidateSourceRepository candidateSourceRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public SourceTrackingService(CandidateSourceRepository candidateSourceRepository,
                                 JobApplicationRepository jobApplicationRepository) {
        this.candidateSourceRepository = candidateSourceRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional
    public CandidateSource trackSource(TrackSourceRequest request) {
        if (candidateSourceRepository.existsByApplicationId(request.getApplicationId())) {
            throw new DuplicateResourceException("Source already tracked for this application");
        }

        JobApplication application = jobApplicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", request.getApplicationId()));

        CandidateSource candidateSource = CandidateSource.builder()
                .application(application)
                .source(request.getSource())
                .sourceCampaign(request.getSourceCampaign())
                .costPerClick(request.getCostPerClick())
                .totalSpend(request.getTotalSpend())
                .build();

        CandidateSource saved = candidateSourceRepository.save(candidateSource);
        log.info("Source tracked for application {}: {}", request.getApplicationId(), request.getSource());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SourceEffectivenessResponse> getSourceEffectiveness() {
        Map<SourceType, Long> totalBySource = toSourceLongMap(candidateSourceRepository.countGroupedBySource());
        Map<SourceType, Long> hiredBySource = toSourceLongMap(candidateSourceRepository.countHiredGroupedBySource());
        Map<SourceType, Long> interviewedBySource = toSourceLongMap(candidateSourceRepository.countInterviewedGroupedBySource());
        Map<SourceType, BigDecimal> spendBySource = toSourceBigDecimalMap(candidateSourceRepository.sumSpendGroupedBySource());

        List<SourceEffectivenessResponse> responses = new ArrayList<>();

        for (SourceType source : SourceType.values()) {
            long total = totalBySource.getOrDefault(source, 0L);
            if (total == 0) continue;

            long hired = hiredBySource.getOrDefault(source, 0L);
            long interviewed = interviewedBySource.getOrDefault(source, 0L);
            BigDecimal spend = spendBySource.getOrDefault(source, BigDecimal.ZERO);
            double conversionRate = total > 0 ? (double) hired / total * 100.0 : 0.0;
            BigDecimal costPerHire = hired > 0 ? spend.divide(BigDecimal.valueOf(hired), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            double avgTimeToHire = calculateAvgTimeToHire(source);
            double roi = calculateROI(spend, hired);

            responses.add(SourceEffectivenessResponse.builder()
                    .source(source)
                    .totalCandidates(total)
                    .interviewed(interviewed)
                    .hired(hired)
                    .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                    .avgTimeToHireDays(avgTimeToHire)
                    .totalSpend(spend)
                    .costPerHire(costPerHire)
                    .roi(roi)
                    .build());
        }

        // Sort by conversion rate descending
        responses.sort(Comparator.comparingDouble(SourceEffectivenessResponse::getConversionRate).reversed());
        return responses;
    }

    @Transactional(readOnly = true)
    public SourceDashboardResponse getSourceDashboard() {
        List<SourceEffectivenessResponse> sources = getSourceEffectiveness();

        long totalCandidates = sources.stream().mapToLong(SourceEffectivenessResponse::getTotalCandidates).sum();
        long totalHired = sources.stream().mapToLong(SourceEffectivenessResponse::getHired).sum();
        BigDecimal totalSpend = sources.stream()
                .map(SourceEffectivenessResponse::getTotalSpend)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double overallConversion = totalCandidates > 0 ? (double) totalHired / totalCandidates * 100.0 : 0.0;

        return SourceDashboardResponse.builder()
                .sources(sources)
                .totalCandidatesAllSources(totalCandidates)
                .totalHiredAllSources(totalHired)
                .totalSpendAllSources(totalSpend)
                .overallConversionRate(Math.round(overallConversion * 100.0) / 100.0)
                .build();
    }

    @Transactional(readOnly = true)
    public SourceEffectivenessResponse getSourceROI(SourceType source) {
        long total = candidateSourceRepository.countBySource(source);
        if (total == 0) {
            throw new ResourceNotFoundException("No data found for source: " + source);
        }

        List<SourceEffectivenessResponse> allEffectiveness = getSourceEffectiveness();
        return allEffectiveness.stream()
                .filter(s -> s.getSource() == source)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No effectiveness data for source: " + source));
    }

    @Transactional(readOnly = true)
    public List<SourceEffectivenessResponse> getTopSources(int limit) {
        List<SourceEffectivenessResponse> all = getSourceEffectiveness();
        return all.stream().limit(limit).collect(Collectors.toList());
    }

    private double calculateAvgTimeToHire(SourceType source) {
        List<CandidateSource> hiredSources = candidateSourceRepository.findHiredBySource(source);
        if (hiredSources.isEmpty()) return 0.0;

        double totalDays = 0;
        int count = 0;
        for (CandidateSource cs : hiredSources) {
            JobApplication app = cs.getApplication();
            if (app.getAppliedAt() != null && app.getStatusUpdatedAt() != null) {
                long days = Duration.between(app.getAppliedAt(), app.getStatusUpdatedAt()).toDays();
                totalDays += days;
                count++;
            }
        }

        return count > 0 ? Math.round((totalDays / count) * 100.0) / 100.0 : 0.0;
    }

    private double calculateROI(BigDecimal spend, long hired) {
        if (spend.compareTo(BigDecimal.ZERO) == 0 || hired == 0) return 0.0;
        // Simple ROI: (value generated - cost) / cost * 100
        // Assuming each hire has an estimated value of $10,000 for simplicity
        BigDecimal estimatedValuePerHire = new BigDecimal("10000");
        BigDecimal totalValue = estimatedValuePerHire.multiply(BigDecimal.valueOf(hired));
        BigDecimal roi = totalValue.subtract(spend).divide(spend, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return roi.doubleValue();
    }

    private Map<SourceType, Long> toSourceLongMap(List<Object[]> results) {
        Map<SourceType, Long> map = new EnumMap<>(SourceType.class);
        for (Object[] row : results) {
            SourceType source = (SourceType) row[0];
            long count = ((Number) row[1]).longValue();
            map.put(source, count);
        }
        return map;
    }

    private Map<SourceType, BigDecimal> toSourceBigDecimalMap(List<Object[]> results) {
        Map<SourceType, BigDecimal> map = new EnumMap<>(SourceType.class);
        for (Object[] row : results) {
            SourceType source = (SourceType) row[0];
            BigDecimal value = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
            map.put(source, value);
        }
        return map;
    }
}
