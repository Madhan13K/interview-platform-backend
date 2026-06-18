package com.interview_platform_backend.interview_platform_backend.dashboard.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewFeedBack;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewFeedbackRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.dashboard.dto.CandidateDashboard;
import com.interview_platform_backend.interview_platform_backend.dashboard.dto.DashboardStats;
import com.interview_platform_backend.interview_platform_backend.dashboard.dto.InterviewerDashboard;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.questionbank.repository.QuestionRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    public DashboardService(InterviewRepository interviewRepository,
                            InterviewFeedbackRepository feedbackRepository,
                            UserRepository userRepository,
                            QuestionRepository questionRepository) {
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }

    public DashboardStats getAdminStats() {
        long totalInterviews = interviewRepository.count();
        long totalUsers = userRepository.count();
        long totalQuestions = questionRepository.count();

        // Interview breakdown by status
        List<Interview> allInterviews = interviewRepository.findAll();
        Map<String, Long> byStatus = allInterviews.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getStatus().name(),
                        Collectors.counting()
                ));

        // Upcoming (next 7 days)
        Instant now = Instant.now();
        Instant nextWeek = now.plus(7, ChronoUnit.DAYS);
        long upcoming = allInterviews.stream()
                .filter(i -> i.getStartTime().isAfter(now) && i.getStartTime().isBefore(nextWeek))
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED)
                .count();

        // Today's interviews
        Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant todayEnd = todayStart.plus(1, ChronoUnit.DAYS);
        long today = allInterviews.stream()
                .filter(i -> i.getStartTime().isAfter(todayStart) && i.getStartTime().isBefore(todayEnd))
                .count();

        // Feedback stats
        List<InterviewFeedBack> allFeedback = feedbackRepository.findAll();
        long totalFeedback = allFeedback.size();
        double avgRating = allFeedback.stream()
                .mapToInt(InterviewFeedBack::getRating)
                .average()
                .orElse(0.0);

        // Recent activity
        List<DashboardStats.RecentActivity> recentActivities = allInterviews.stream()
                .sorted(Comparator.comparing(Interview::getCreatedAt).reversed())
                .limit(10)
                .map(i -> DashboardStats.RecentActivity.builder()
                        .type("INTERVIEW_" + i.getStatus().name())
                        .description(i.getTitle() + " - " + i.getCandidate().getFirstName() + " " + i.getCandidate().getLastName())
                        .timestamp(i.getCreatedAt().toString())
                        .build())
                .toList();

        return DashboardStats.builder()
                .totalInterviews(totalInterviews)
                .totalUsers(totalUsers)
                .totalQuestions(totalQuestions)
                .interviewsByStatus(byStatus)
                .upcomingInterviews(upcoming)
                .todayInterviews(today)
                .totalFeedbackSubmitted(totalFeedback)
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .recentActivities(recentActivities)
                .build();
    }

    public InterviewerDashboard getInterviewerDashboard(UUID interviewerId) {
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        List<Interview> interviews = interviewRepository.findByInterviewerId(interviewerId);
        Instant now = Instant.now();

        long total = interviews.size();
        long completed = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                .count();
        long upcoming = interviews.stream()
                .filter(i -> i.getStartTime().isAfter(now))
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED)
                .count();

        // Pending feedback = completed interviews without feedback from this interviewer
        List<InterviewFeedBack> myFeedback = feedbackRepository.findByInterviewer(interviewer);
        Set<UUID> feedbackInterviewIds = myFeedback.stream()
                .map(f -> f.getInterview().getId())
                .collect(Collectors.toSet());
        long pendingFeedback = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                .filter(i -> !feedbackInterviewIds.contains(i.getId()))
                .count();

        double avgRating = myFeedback.stream()
                .mapToInt(InterviewFeedBack::getRating)
                .average()
                .orElse(0.0);

        List<InterviewerDashboard.UpcomingInterview> nextInterviews = interviews.stream()
                .filter(i -> i.getStartTime().isAfter(now))
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED)
                .sorted(Comparator.comparing(Interview::getStartTime))
                .limit(5)
                .map(i -> InterviewerDashboard.UpcomingInterview.builder()
                        .interviewId(i.getId())
                        .title(i.getTitle())
                        .candidateName(i.getCandidate().getFirstName() + " " + i.getCandidate().getLastName())
                        .startTime(i.getStartTime())
                        .endTime(i.getEndTime())
                        .status(i.getStatus().name())
                        .build())
                .toList();

        return InterviewerDashboard.builder()
                .interviewerId(interviewerId)
                .interviewerName(interviewer.getFirstName() + " " + interviewer.getLastName())
                .totalAssignedInterviews(total)
                .completedInterviews(completed)
                .upcomingInterviews(upcoming)
                .pendingFeedback(pendingFeedback)
                .averageRatingGiven(Math.round(avgRating * 10.0) / 10.0)
                .nextInterviews(nextInterviews)
                .build();
    }

    public CandidateDashboard getCandidateDashboard(UUID candidateId) {
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", candidateId));

        List<Interview> interviews = interviewRepository.findByCandidateId(candidateId);
        Instant now = Instant.now();

        long total = interviews.size();
        long completed = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED).count();
        long upcoming = interviews.stream()
                .filter(i -> i.getStartTime().isAfter(now))
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED).count();
        long cancelled = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.CANCELLED).count();

        List<CandidateDashboard.UpcomingInterview> nextInterviews = interviews.stream()
                .filter(i -> i.getStartTime().isAfter(now))
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED)
                .sorted(Comparator.comparing(Interview::getStartTime))
                .limit(5)
                .map(i -> {
                    String interviewerNames = i.getInterviewers() != null
                            ? i.getInterviewers().stream()
                                .map(ii -> ii.getInterviewer().getFirstName() + " " + ii.getInterviewer().getLastName())
                                .collect(Collectors.joining(", "))
                            : "";
                    return CandidateDashboard.UpcomingInterview.builder()
                            .interviewId(i.getId())
                            .title(i.getTitle())
                            .interviewerNames(interviewerNames)
                            .startTime(i.getStartTime())
                            .endTime(i.getEndTime())
                            .status(i.getStatus().name())
                            .meetingLink(i.getMeetingLink())
                            .build();
                })
                .toList();

        return CandidateDashboard.builder()
                .candidateId(candidateId)
                .candidateName(candidate.getFirstName() + " " + candidate.getLastName())
                .totalInterviews(total)
                .completedInterviews(completed)
                .upcomingInterviews(upcoming)
                .cancelledInterviews(cancelled)
                .nextInterviews(nextInterviews)
                .build();
    }
}

