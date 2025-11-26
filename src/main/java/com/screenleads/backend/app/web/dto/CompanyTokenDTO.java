package com.screenleads.backend.app.web.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyTokenDTO {
    private Long id;
    private Long companyId;
    private String token;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String descripcion;
}
