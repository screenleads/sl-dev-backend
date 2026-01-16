package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.domain.model.ExportFormat;
import com.screenleads.backend.app.domain.model.ExportType;
import com.screenleads.backend.app.web.dto.DataExportDTO;

public interface DataExportService {
    
    List<DataExportDTO> getAllExports();
    
    Optional<DataExportDTO> getExportById(Long id);
    
    List<DataExportDTO> getExportsByCompany(Long companyId);
    
    List<DataExportDTO> getExportsByUser(Long userId);
    
    DataExportDTO createExport(DataExportDTO dto);
    
    DataExportDTO updateExport(Long id, DataExportDTO dto);
    
    void deleteExport(Long id);
    
    DataExportDTO requestExport(Long companyId, Long userId, ExportType exportType, ExportFormat exportFormat, String filters, String purpose);
    
    DataExportDTO markAsStarted(Long id);
    
    DataExportDTO markAsCompleted(Long id, String fileUrl, Integer totalRecords, Long fileSizeBytes);
    
    DataExportDTO markAsFailed(Long id, String errorMessage);
    
    DataExportDTO recordDownload(Long id);
    
    void cleanupExpiredExports();
}
