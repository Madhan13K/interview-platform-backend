package com.interview_platform_backend.interview_platform_backend.offer.esignature;

import com.interview_platform_backend.interview_platform_backend.offer.entity.ESignatureStatus;
import com.interview_platform_backend.interview_platform_backend.offer.entity.OfferLetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Simulated HelloSign (Dropbox Sign) e-signature service implementation.
 * TODO: Replace with actual Dropbox Sign API integration.
 */
@Service("helloSignService")
public class HelloSignService implements ESignatureService {

    private static final Logger log = LoggerFactory.getLogger(HelloSignService.class);

    @Override
    public String sendForSignature(OfferLetter offerLetter) {
        // TODO: Integrate with Dropbox Sign API
        // 1. Create signature request with offer document
        // 2. Add signer (candidate email)
        // 3. Send signature request
        String signatureRequestId = "HELLOSIGN-SR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("HelloSign: Simulated sending offer {} for signature. Request ID: {}",
                offerLetter.getId(), signatureRequestId);
        return signatureRequestId;
    }

    @Override
    public ESignatureStatus getSignatureStatus(String envelopeId) {
        // TODO: Call Dropbox Sign API GET /signature_request/{id} to check status
        log.info("HelloSign: Simulated status check for request {}", envelopeId);
        return ESignatureStatus.SENT;
    }

    @Override
    public String getSignedDocumentUrl(String envelopeId) {
        // TODO: Call Dropbox Sign API to get the signed document download URL
        String url = "https://app.hellosign.com/documents/" + envelopeId + "/download";
        log.info("HelloSign: Simulated document URL for request {}: {}", envelopeId, url);
        return url;
    }
}
