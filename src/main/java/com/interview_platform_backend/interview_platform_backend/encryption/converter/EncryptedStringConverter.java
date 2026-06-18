package com.interview_platform_backend.interview_platform_backend.encryption.converter;

import com.interview_platform_backend.interview_platform_backend.encryption.service.FieldEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that automatically encrypts/decrypts String fields.
 * 
 * Usage: Apply to any entity field containing PII:
 * 
 * <pre>
 * {@code
 * @Convert(converter = EncryptedStringConverter.class)
 * @Column(name = "phone_number")
 * private String phoneNumber;
 * }
 * </pre>
 * 
 * The field will be stored encrypted in the database and automatically
 * decrypted when read back into the entity.
 * 
 * Backward compatible: if a value doesn't have the "ENC:" prefix, 
 * it's returned as-is (allows gradual migration of existing data).
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static FieldEncryptionService encryptionService;

    /**
     * Spring will inject the service here.
     * We use a static setter because JPA instantiates converters without Spring context.
     */
    public EncryptedStringConverter(FieldEncryptionService fieldEncryptionService) {
        EncryptedStringConverter.encryptionService = fieldEncryptionService;
    }

    // Default no-arg constructor for JPA
    public EncryptedStringConverter() {
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty() || encryptionService == null) {
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || encryptionService == null) {
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}
