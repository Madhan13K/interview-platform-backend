package com.interview_platform_backend.interview_platform_backend.offer.esignature;

import com.interview_platform_backend.interview_platform_backend.offer.entity.ESignatureStatus;
import com.interview_platform_backend.interview_platform_backend.offer.entity.OfferLetter;

public interface ESignatureService {

    /**
     * Sends the offer letter document for e-signature.
     *
     * @param offerLetter the offer letter to send for signing
     * @return the envelope/document ID from the e-signature provider
     */
    String sendForSignature(OfferLetter offerLetter);

    /**
     * Gets the current signature status from the provider.
     *
     * @param envelopeId the provider's envelope/document ID
     * @return the current e-signature status
     */
    ESignatureStatus getSignatureStatus(String envelopeId);

    /**
     * Gets the URL to the signed document.
     *
     * @param envelopeId the provider's envelope/document ID
     * @return the URL to download/view the signed document
     */
    String getSignedDocumentUrl(String envelopeId);
}
