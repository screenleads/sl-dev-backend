package com.screenleads.backend.app.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "role" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String role; // p.ej. ROLE_ADMIN

    private String description;

    /**
     * Jerarquía (1 = más alto privilegio)
     */
    @Column(nullable = false)
    private Integer level;

    // ======= PERMISOS POR ENTIDAD (CRUD) =======
    // User
    @Builder.Default
    private boolean userRead = false;
    @Builder.Default
    private boolean userCreate = false;
    @Builder.Default
    private boolean userUpdate = false;
    @Builder.Default
    private boolean userDelete = false;

    // Company
    @Builder.Default
    private boolean companyRead = false;
    @Builder.Default
    private boolean companyCreate = false;
    @Builder.Default
    private boolean companyUpdate = false;
    @Builder.Default
    private boolean companyDelete = false;

    // Device
    @Builder.Default
    private boolean deviceRead = false;
    @Builder.Default
    private boolean deviceCreate = false;
    @Builder.Default
    private boolean deviceUpdate = false;
    @Builder.Default
    private boolean deviceDelete = false;

    // DeviceType
    @Builder.Default
    private boolean deviceTypeRead = false;
    @Builder.Default
    private boolean deviceTypeCreate = false;
    @Builder.Default
    private boolean deviceTypeUpdate = false;
    @Builder.Default
    private boolean deviceTypeDelete = false;

    // Media
    @Builder.Default
    private boolean mediaRead = false;
    @Builder.Default
    private boolean mediaCreate = false;
    @Builder.Default
    private boolean mediaUpdate = false;
    @Builder.Default
    private boolean mediaDelete = false;

    // MediaType
    @Builder.Default
    private boolean mediaTypeRead = false;
    @Builder.Default
    private boolean mediaTypeCreate = false;
    @Builder.Default
    private boolean mediaTypeUpdate = false;
    @Builder.Default
    private boolean mediaTypeDelete = false;

    // Promotion
    @Builder.Default
    private boolean promotionRead = false;
    @Builder.Default
    private boolean promotionCreate = false;
    @Builder.Default
    private boolean promotionUpdate = false;
    @Builder.Default
    private boolean promotionDelete = false;

    // Advice
    @Builder.Default
    private boolean adviceRead = false;
    @Builder.Default
    private boolean adviceCreate = false;
    @Builder.Default
    private boolean adviceUpdate = false;
    @Builder.Default
    private boolean adviceDelete = false;

    // AppVersion
    @Builder.Default
    private boolean appVersionRead = false;
    @Builder.Default
    private boolean appVersionCreate = false;
    @Builder.Default
    private boolean appVersionUpdate = false;
    @Builder.Default
    private boolean appVersionDelete = false;

    // ======= PERMISOS PARA Role (la propia entidad) =======
    @Builder.Default
    private boolean roleRead = false;
    @Builder.Default
    private boolean roleCreate = false;
    @Builder.Default
    private boolean roleUpdate = false;
    @Builder.Default
    private boolean roleDelete = false;

    // (si creaste PromotionLead como entidad)
    // ======= PERMISOS PARA PromotionLead =======
    @Builder.Default
    private boolean promotionLeadRead = false;
    @Builder.Default
    private boolean promotionLeadCreate = false;
    @Builder.Default
    private boolean promotionLeadUpdate = false;
    @Builder.Default
    private boolean promotionLeadDelete = false;
}
