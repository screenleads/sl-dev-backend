package com.screenleads.backend.app.domain.model;

/**
 * Formatos de archivo para exportaciones de datos
 */
public enum ExportFormat {
    /**
     * Comma-Separated Values - ideal para Excel
     */
    CSV,
    
    /**
     * JavaScript Object Notation - formato estructurado
     */
    JSON,
    
    /**
     * Excel moderno (.xlsx)
     */
    XLSX,
    
    /**
     * Extensible Markup Language
     */
    XML
}
