package com.screenleads.backend.app.web.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityInfo {
    private String entityName;
    private String className;
    private String tableName;
    private String idType;
    private Map<String, String> attributes = new LinkedHashMap<>();
    private Long rowCount; // null si no se pidió

    public EntityInfo() {
    }

    public EntityInfo(String entityName, String className, String tableName, String idType,
            Map<String, String> attributes, Long rowCount) {
        this.entityName = entityName;
        this.className = className;
        this.tableName = tableName;
        this.idType = idType;
        this.attributes = attributes;
        this.rowCount = rowCount;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }
}
