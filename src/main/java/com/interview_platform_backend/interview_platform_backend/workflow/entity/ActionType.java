package com.interview_platform_backend.interview_platform_backend.workflow.entity;

public enum ActionType {
    ADVANCE_PIPELINE_STAGE,
    SEND_EMAIL,
    CHANGE_INTERVIEW_STATUS,
    REJECT_CANDIDATE,
    NOTIFY_RECRUITER,
    WEBHOOK_CALL
}
