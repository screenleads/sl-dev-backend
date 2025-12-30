// src/main/java/com/screenleads/backend/app/web/dto/VerifyTokenResponse.java
package com.screenleads.backend.app.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyTokenResponse {

    private Boolean valid;
    private String message;
    private String userEmail;
}
