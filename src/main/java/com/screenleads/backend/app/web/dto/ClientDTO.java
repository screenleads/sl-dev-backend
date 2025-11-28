package com.screenleads.backend.app.web.dto;

import java.util.List;

public class ClientDTO {
    private Long id;
    private String clientId;
    private String name;
    private boolean active;
    private List<ApiKeyDTO> apiKeys;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<ApiKeyDTO> getApiKeys() { return apiKeys; }
    public void setApiKeys(List<ApiKeyDTO> apiKeys) { this.apiKeys = apiKeys; }
}
