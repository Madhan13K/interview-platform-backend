-- V24: Offer Letter Management - Offers, approval workflows, e-signature tracking
CREATE TABLE offer_letters (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_position_id         UUID NOT NULL REFERENCES job_positions(id) ON DELETE CASCADE,
    created_by              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    offer_content           TEXT,
    salary_offered          TEXT,
    salary_currency         VARCHAR(10) DEFAULT 'USD',
    bonus_amount            TEXT,
    start_date              DATE,
    expires_at              TIMESTAMP WITH TIME ZONE,
    sent_at                 TIMESTAMP WITH TIME ZONE,
    viewed_at               TIMESTAMP WITH TIME ZONE,
    responded_at            TIMESTAMP WITH TIME ZONE,
    candidate_response      TEXT,
    esignature_provider     VARCHAR(30) DEFAULT 'NONE',
    esignature_envelope_id  VARCHAR(200),
    esignature_status       VARCHAR(30),
    esignature_signed_at    TIMESTAMP WITH TIME ZONE,
    esignature_document_url VARCHAR(500),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_offer_letters_candidate ON offer_letters(candidate_id);
CREATE INDEX idx_offer_letters_position ON offer_letters(job_position_id);
CREATE INDEX idx_offer_letters_status ON offer_letters(status);
CREATE INDEX idx_offer_letters_created_by ON offer_letters(created_by);

CREATE TABLE offer_approvals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_letter_id UUID NOT NULL REFERENCES offer_letters(id) ON DELETE CASCADE,
    approver_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    comments        TEXT,
    approval_order  INTEGER NOT NULL DEFAULT 1,
    requested_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    responded_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_offer_approvals_offer ON offer_approvals(offer_letter_id);
CREATE INDEX idx_offer_approvals_approver ON offer_approvals(approver_id);
CREATE INDEX idx_offer_approvals_status ON offer_approvals(status);
