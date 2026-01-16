package com.screenleads.backend.app.domain.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

/**
 * Registro de exportaciones de datos para remarketing y cumplimiento GDPR.
 * Permite auditoría completa de quién exportó qué datos y para qué propósito.
 */
@Entity
@Table(name = "data_export",
    indexes = {
        @Index(name = "ix_dataexport_company", columnList = "company_id"),
        @Index(name = "ix_dataexport_created", columnList = "created_at"),
        @Index(name = "ix_dataexport_status", columnList = "status"),
        @Index(name = "ix_dataexport_type", columnList = "export_type"),
        @Index(name = "ix_dataexport_requestedby", columnList = "requested_by_user_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataExport extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_dataexport_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id",
        foreignKey = @ForeignKey(name = "fk_dataexport_user"))
    private User requestedBy;

    // === Tipo de exportación ===
    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 50)
    private ExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 20)
    private ExportFormat exportFormat;

    // === Filtros aplicados ===
    @Lob
    @Column(name = "filters", columnDefinition = "TEXT")
    private String filters; // JSON con filtros aplicados
    
    /* Ejemplo:
    {
      "promotionIds": [1, 5, 7],
      "startDate": "2026-01-01",
      "endDate": "2026-01-31",
      "onlyVerified": true,
      "minLeadScore": 70,
      "includeFields": ["email", "phone", "firstName", "lastName"]
    }
    */

    // === Resultados ===
    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "file_url", length = 2048)
    private String fileUrl; // URL al archivo generado (puede ser S3, Azure Blob, etc.)

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "expires_at")
    private Instant expiresAt; // URL temporal que expira

    // === Estado ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ExportStatus status = ExportStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // === Propósito (para auditoría GDPR) ===
    @Column(name = "purpose", length = 255)
    private String purpose; // "remarketing", "gdpr_request", "analytics", "backup"

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;
    
    @Column(name = "last_download_at")
    private Instant lastDownloadAt;

    // === Métodos helper ===
    
    /**
     * Marca la exportación como iniciada
     */
    public void markAsStarted() {
        this.status = ExportStatus.PROCESSING;
        this.startedAt = Instant.now();
    }
    
    /**
     * Marca la exportación como completada
     */
    public void markAsCompleted(String fileUrl, Long fileSizeBytes, Integer totalRecords) {
        this.status = ExportStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.fileUrl = fileUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.totalRecords = totalRecords;
    }
    
    /**
     * Marca la exportación como fallida
     */
    public void markAsFailed(String errorMessage) {
        this.status = ExportStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
    }
    
    /**
     * Registra una descarga
     */
    public void recordDownload() {
        if (this.downloadedAt == null) {
            this.downloadedAt = Instant.now();
        }
        this.lastDownloadAt = Instant.now();
        this.downloadCount++;
    }
    
    /**
     * Verifica si la exportación está disponible para descargar
     */
    public boolean isAvailableForDownload() {
        if (status != ExportStatus.COMPLETED) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return fileUrl != null;
    }
    
    /**
     * Verifica si la exportación ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Calcula el tiempo de procesamiento en segundos
     */
    public Long getProcessingTimeSeconds() {
        if (startedAt == null || completedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }
    
    /**
     * Crea una exportación de clientes para remarketing
     */
    public static DataExport createRemarketingExport(
        Company company,
        User requestedBy,
        ExportFormat format,
        String filters
    ) {
        return DataExport.builder()
            .company(company)
            .requestedBy(requestedBy)
            .exportType(ExportType.CUSTOMERS)
            .exportFormat(format)
            .filters(filters)
            .purpose("remarketing")
            .status(ExportStatus.PENDING)
            .build();
    }
    
    /**
     * Crea una exportación de canjes
     */
    public static DataExport createRedemptionsExport(
        Company company,
        User requestedBy,
        ExportFormat format,
        String filters
    ) {
        return DataExport.builder()
            .company(company)
            .requestedBy(requestedBy)
            .exportType(ExportType.REDEMPTIONS)
            .exportFormat(format)
            .filters(filters)
            .purpose("analytics")
            .status(ExportStatus.PENDING)
            .build();
    }
    
    /**
     * Crea una exportación completa para solicitud GDPR
     */
    public static DataExport createGDPRExport(
        Company company,
        User requestedBy,
        String filters
    ) {
        return DataExport.builder()
            .company(company)
            .requestedBy(requestedBy)
            .exportType(ExportType.GDPR_REQUEST)
            .exportFormat(ExportFormat.JSON)
            .filters(filters)
            .purpose("gdpr_request")
            .status(ExportStatus.PENDING)
            .build();
    }
}
