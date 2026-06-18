package com.interview_platform_backend.interview_platform_backend.security.mfa.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.mfa.dto.MfaSetupResponse;
import com.interview_platform_backend.interview_platform_backend.security.mfa.entity.UserMfa;
import com.interview_platform_backend.interview_platform_backend.security.mfa.repository.UserMfaRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final String ISSUER = "InterviewPlatform";

    private final UserMfaRepository userMfaRepository;
    private final UserRepository userRepository;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;

    public MfaService(UserMfaRepository userMfaRepository, UserRepository userRepository) {
        this.userMfaRepository = userMfaRepository;
        this.userRepository = userRepository;
        this.secretGenerator = new DefaultSecretGenerator();

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    public MfaSetupResponse setupMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Remove existing unenabled setup if any
        userMfaRepository.findByUserId(userId).ifPresent(existing -> {
            if (Boolean.TRUE.equals(existing.getIsEnabled())) {
                throw new BadRequestException("MFA is already enabled for this user");
            }
            userMfaRepository.delete(existing);
        });

        String secret = secretGenerator.generate();
        List<String> backupCodes = generateBackupCodes();

        UserMfa userMfa = UserMfa.builder()
                .user(user)
                .secret(secret)
                .isEnabled(false)
                .backupCodes(backupCodes.toArray(new String[0]))
                .createdAt(Instant.now())
                .build();

        userMfaRepository.save(userMfa);

        String qrCodeUri = buildOtpAuthUri(secret, user.getEmail());

        log.info("MFA setup initiated for user {}", userId);

        return MfaSetupResponse.builder()
                .secretKey(secret)
                .qrCodeUri(qrCodeUri)
                .backupCodes(backupCodes)
                .build();
    }

    public boolean verifyAndEnable(UUID userId, String code) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("MFA setup not found. Please initiate setup first."));

        if (Boolean.TRUE.equals(userMfa.getIsEnabled())) {
            throw new BadRequestException("MFA is already enabled");
        }

        if (!codeVerifier.isValidCode(userMfa.getSecret(), code)) {
            throw new BadRequestException("Invalid verification code");
        }

        userMfa.setIsEnabled(true);
        userMfa.setVerifiedAt(Instant.now());
        userMfaRepository.save(userMfa);

        log.info("MFA enabled for user {}", userId);
        return true;
    }

    public boolean verifyCode(UUID userId, String code) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("MFA is not configured for this user"));

        if (!Boolean.TRUE.equals(userMfa.getIsEnabled())) {
            throw new BadRequestException("MFA is not enabled");
        }

        // First try TOTP code
        if (codeVerifier.isValidCode(userMfa.getSecret(), code)) {
            return true;
        }

        // Then try backup codes
        if (userMfa.getBackupCodes() != null) {
            List<String> codes = new ArrayList<>(Arrays.asList(userMfa.getBackupCodes()));
            if (codes.contains(code)) {
                codes.remove(code);
                userMfa.setBackupCodes(codes.toArray(new String[0]));
                userMfaRepository.save(userMfa);
                log.info("Backup code used for user {}", userId);
                return true;
            }
        }

        return false;
    }

    public void disableMfa(UUID userId) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("MFA is not configured for this user"));

        userMfaRepository.delete(userMfa);
        log.info("MFA disabled for user {}", userId);
    }

    @Transactional(readOnly = true)
    public boolean isMfaEnabled(UUID userId) {
        return userMfaRepository.existsByUserIdAndIsEnabledTrue(userId);
    }

    public List<String> regenerateBackupCodes(UUID userId) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("MFA is not configured for this user"));

        if (!Boolean.TRUE.equals(userMfa.getIsEnabled())) {
            throw new BadRequestException("MFA must be enabled to regenerate backup codes");
        }

        List<String> newCodes = generateBackupCodes();
        userMfa.setBackupCodes(newCodes.toArray(new String[0]));
        userMfaRepository.save(userMfa);

        log.info("Backup codes regenerated for user {}", userId);
        return newCodes;
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            codes.add(String.format("%08d", random.nextInt(100000000)));
        }
        return codes;
    }

    private String buildOtpAuthUri(String secret, String email) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                ISSUER, email, secret, ISSUER);
    }
}
