package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("perm")
public class PermissionService {

    private final UserRepository userRepo;

    public PermissionService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public boolean can(String entity, String action) {
        User u = currentUser();
        if (u == null)
            return false;
        for (Role r : u.getRoles()) {
            if (switch (entity.toLowerCase()) {
                case "user" -> switch (action.toLowerCase()) {
                    case "read" -> r.isUserRead();
                    case "create" -> r.isUserCreate();
                    case "update" -> r.isUserUpdate();
                    case "delete" -> r.isUserDelete();
                    default -> false;
                };
                case "company" -> switch (action.toLowerCase()) {
                    case "read" -> r.isCompanyRead();
                    case "create" -> r.isCompanyCreate();
                    case "update" -> r.isCompanyUpdate();
                    case "delete" -> r.isCompanyDelete();
                    default -> false;
                };
                case "device" -> switch (action.toLowerCase()) {
                    case "read" -> r.isDeviceRead();
                    case "create" -> r.isDeviceCreate();
                    case "update" -> r.isDeviceUpdate();
                    case "delete" -> r.isDeviceDelete();
                    default -> false;
                };
                case "devicetype" -> switch (action.toLowerCase()) {
                    case "read" -> r.isDeviceTypeRead();
                    case "create" -> r.isDeviceTypeCreate();
                    case "update" -> r.isDeviceTypeUpdate();
                    case "delete" -> r.isDeviceTypeDelete();
                    default -> false;
                };
                case "media" -> switch (action.toLowerCase()) {
                    case "read" -> r.isMediaRead();
                    case "create" -> r.isMediaCreate();
                    case "update" -> r.isMediaUpdate();
                    case "delete" -> r.isMediaDelete();
                    default -> false;
                };
                case "mediatype" -> switch (action.toLowerCase()) {
                    case "read" -> r.isMediaTypeRead();
                    case "create" -> r.isMediaTypeCreate();
                    case "update" -> r.isMediaTypeUpdate();
                    case "delete" -> r.isMediaTypeDelete();
                    default -> false;
                };
                case "promotion" -> switch (action.toLowerCase()) {
                    case "read" -> r.isPromotionRead();
                    case "create" -> r.isPromotionCreate();
                    case "update" -> r.isPromotionUpdate();
                    case "delete" -> r.isPromotionDelete();
                    default -> false;
                };
                case "advice" -> switch (action.toLowerCase()) {
                    case "read" -> r.isAdviceRead();
                    case "create" -> r.isAdviceCreate();
                    case "update" -> r.isAdviceUpdate();
                    case "delete" -> r.isAdviceDelete();
                    default -> false;
                };
                case "appversion" -> switch (action.toLowerCase()) {
                    case "read" -> r.isAppVersionRead();
                    case "create" -> r.isAppVersionCreate();
                    case "update" -> r.isAppVersionUpdate();
                    case "delete" -> r.isAppVersionDelete();
                    default -> false;
                };
                default -> false;
            })
                return true;
        }
        return false;
    }

    public Integer effectiveLevel() {
        User u = currentUser();
        if (u == null || u.getRoles().isEmpty())
            return Integer.MAX_VALUE;
        return u.getRoles().stream().map(Role::getLevel).min(Integer::compareTo).orElse(Integer.MAX_VALUE);
    }

    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated())
            return null;
        Object p = a.getPrincipal();
        if (p instanceof User u)
            return u;
        if (p instanceof org.springframework.security.core.userdetails.User ud) {
            return userRepo.findByUsername(ud.getUsername()).orElse(null);
        }
        if (p instanceof String username) {
            return userRepo.findByUsername(username).orElse(null);
        }
        return null;
    }
}
