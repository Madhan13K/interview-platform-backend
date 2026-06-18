package com.interview_platform_backend.interview_platform_backend.encryption.converter;

import com.interview_platform_backend.interview_platform_backend.encryption.service.FieldEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * JPA AttributeConverter that encrypts/decrypts BigDecimal fields (salary, compensation).
 * 
 * Usage:
 * <pre>
 * {@code
 * @Convert(converter = EncryptedBigDecimalConverter.class)
 * @Column(name = "salary_min")
 * private BigDecimal salaryMin;
 * }
 * </pre>
 * 
 * Stores the BigDecimal as an encrypted string in a TEXT/VARCHAR column.
 */
@Converter
@Component
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    private static FieldEncryptionService encryptionService;

    public EncryptedBigDecimalConverter(FieldEncryptionService fieldEncryptionService) {
        EncryptedBigDecimalConverter.encryptionService = fieldEncryptionService;
    }

    public EncryptedBigDecimalConverter() {
    }

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null || encryptionService == null) {
            return null;
        }
        return encryptionService.encrypt(attribute.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || encryptionService == null) {
            return null;
        }
        String decrypted = encryptionService.decrypt(dbData);
        try {
            return new BigDecimal(decrypted);
        } catch (NumberFormatException e) {
            // If decryption fails or value isn't a valid number, return null
            return null;
        }
    }
}
