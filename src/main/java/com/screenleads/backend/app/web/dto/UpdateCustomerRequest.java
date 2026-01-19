package com.screenleads.backend.app.web.dto;

import java.time.LocalDate;

import com.screenleads.backend.app.domain.model.Gender;
import com.screenleads.backend.app.domain.model.UserSegment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar Customer existente
 * Todos los campos son opcionales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Email
    @Size(max = 320)
    private String email;

    @Size(max = 50)
    private String phone;

    private LocalDate birthDate;

    private Gender gender;

    // Ubicación
    @Size(max = 100)
    private String city;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 100)
    private String country;

    @Size(max = 10)
    private String preferredLanguage;

    // Perfiles sociales (JSON)
    private String socialProfiles;

    // Consentimientos
    private Boolean marketingOptIn;
    private Boolean dataProcessingConsent;
    private Boolean thirdPartyDataSharing;

    // Segmentación (solo admin puede modificar)
    private String tags;

    @Min(0)
    @Max(100)
    private Integer engagementScore;

    private UserSegment segment;
}
