package com.interview_platform_backend.interview_platform_backend.notification.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SMS Notification Service.
 *
 * Integrates with SMS providers (Twilio, AWS SNS, etc.)
 * Currently logs SMS messages. Replace with actual provider implementation.
 *
 * To integrate Twilio:
 * 1. Add dependency: com.twilio.sdk:twilio
 * 2. Set twilio.account-sid, twilio.auth-token, twilio.from-number in application.yml
 * 3. Uncomment the Twilio implementation below
 */
@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:log}")
    private String smsProvider;

    @Async
    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS disabled. Would send to={}, message={}", phoneNumber, message);
            return;
        }

        switch (smsProvider.toLowerCase()) {
            case "twilio" -> sendViaTwilio(phoneNumber, message);
            case "sns" -> sendViaSns(phoneNumber, message);
            default -> log.info("SMS (log provider): to={}, message={}", phoneNumber, message);
        }
    }

    private void sendViaTwilio(String phoneNumber, String message) {
        // TODO: Implement Twilio integration
        // Twilio.init(accountSid, authToken);
        // Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber(fromNumber), message).create();
        log.info("Twilio SMS sent to {}: {}", phoneNumber, message);
    }

    private void sendViaSns(String phoneNumber, String message) {
        // TODO: Implement AWS SNS integration
        // snsClient.publish(PublishRequest.builder().phoneNumber(phoneNumber).message(message).build());
        log.info("AWS SNS SMS sent to {}: {}", phoneNumber, message);
    }
}

