package com.interview_platform_backend.interview_platform_backend.offer.esignature;

import com.interview_platform_backend.interview_platform_backend.offer.entity.ESignatureStatus;
import com.interview_platform_backend.interview_platform_backend.offer.entity.OfferLetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Simulated DocuSign e-signature service implementation.
 * TODO: Replace with actual DocuSign API integration using docusign-esign-java SDK.
 */
@Service("docuSignService")
public class DocuSignService implements ESignatureService {

    private static final Logger log = LoggerFactory.getLogger(DocuSignService.class);

    @Override
    public String sendForSignature(OfferLetter offerLetter) {
        // TODO: Integrate with DocuSign REST API
        // 1. Create envelope with offer document
        // 2. Add recipient (candidate) with signing tabs
        // 3. Send envelope
        String envelopeId = "DOCUSIGN-ENV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("DocuSign: Simulated sending offer {} for signature. Envelope ID: {}",
                offerLetter.getId(), envelopeId);
        return envelopeId;
    }

    @Override
    public ESignatureStatus getSignatureStatus(String envelopeId) {
        // TODO: Call DocuSign API GET /envelopes/{envelopeId} to check status
        log.info("DocuSign: Simulated status check for envelope {}", envelopeId);
        return ESignatureStatus.SENT;
    }

    @Override
    public String getSignedDocumentUrl(String envelopeId) {
        // TODO: Call DocuSign API GET /envelopes/{envelopeId}/documents to get signed PDF
        String url = "https://demo.docusign.net/documents/" + envelopeId + "/signed.pdf";
        log.info("DocuSign: Simulated document URL for envelope {}: {}", envelopeId, url);
        return url;
    }
}
