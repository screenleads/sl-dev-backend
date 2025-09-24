package com.screenleads.backend.app.application.service;

public interface PermissionService {
    boolean can(String resource, String action); // action: "create","read","update","delete"

    int effectiveLevel(); // nivel del rol del usuario actual
}
