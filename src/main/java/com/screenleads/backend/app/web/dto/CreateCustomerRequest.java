package com.screenleads.backend.app.web.dto;

import java.time.LocalDate;
import java.util.Set;

import com.screenleads.backend.app.domain.model.AuthMethod;
import com.screenleads.backend.app.domain.model.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear un nuevo Customer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {
    
    @Size(max = 100)
    private String firstName;
    
    @Size(max = 100)
    private String lastName;
    
    @Email
    @Size(max = 320)
    private String email;
    
    @Size(max = 50)
    private String phone; // Formato E.164: +34612345678
    
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
    private String preferredLanguage; // es, en, fr...
    
    // Método de autenticación usado
    @NotNull
    private AuthMethod authMethod;
    
    // Perfiles sociales (JSON opcional)
    private String socialProfiles;
    
    // Consentimientos
    @Builder.Default
    private Boolean marketingOptIn = false;
    
    @Builder.Default
    private Boolean dataProcessingConsent = false;
    
    @Builder.Default
    private Boolean thirdPartyDataSharing = false;
    
    /**
     * Valida que al menos email o phone estén presentes
     */
    public boolean hasValidIdentifier() {
        return (email != null && !email.isBlank()) || 
               (phone != null && !phone.isBlank());
    }
}
