// com.screenleads.backend.app.web.mapper.RoleMapper
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.web.dto.RoleDTO;

public class RoleMapper {

    public static RoleDTO toDTO(Role r) {
        if (r == null)
            return null;
        return new RoleDTO(
                r.getId(), r.getRole(), r.getDescription(), r.getLevel(),
                r.isUserRead(), r.isUserCreate(), r.isUserUpdate(), r.isUserDelete(),
                r.isCompanyRead(), r.isCompanyCreate(), r.isCompanyUpdate(), r.isCompanyDelete(),
                r.isDeviceRead(), r.isDeviceCreate(), r.isDeviceUpdate(), r.isDeviceDelete(),
                r.isDeviceTypeRead(), r.isDeviceTypeCreate(), r.isDeviceTypeUpdate(), r.isDeviceTypeDelete(),
                r.isMediaRead(), r.isMediaCreate(), r.isMediaUpdate(), r.isMediaDelete(),
                r.isMediaTypeRead(), r.isMediaTypeCreate(), r.isMediaTypeUpdate(), r.isMediaTypeDelete(),
                r.isPromotionRead(), r.isPromotionCreate(), r.isPromotionUpdate(), r.isPromotionDelete(),
                r.isAdviceRead(), r.isAdviceCreate(), r.isAdviceUpdate(), r.isAdviceDelete(),
                r.isAppVersionRead(), r.isAppVersionCreate(), r.isAppVersionUpdate(), r.isAppVersionDelete());
    }

    public static Role toEntity(RoleDTO d) {
        if (d == null)
            return null;
        return Role.builder()
                .id(d.id())
                .role(d.role())
                .description(d.description())
                .level(d.level())
                .userRead(d.userRead()).userCreate(d.userCreate()).userUpdate(d.userUpdate()).userDelete(d.userDelete())
                .companyRead(d.companyRead()).companyCreate(d.companyCreate()).companyUpdate(d.companyUpdate())
                .companyDelete(d.companyDelete())
                .deviceRead(d.deviceRead()).deviceCreate(d.deviceCreate()).deviceUpdate(d.deviceUpdate())
                .deviceDelete(d.deviceDelete())
                .deviceTypeRead(d.deviceTypeRead()).deviceTypeCreate(d.deviceTypeCreate())
                .deviceTypeUpdate(d.deviceTypeUpdate()).deviceTypeDelete(d.deviceTypeDelete())
                .mediaRead(d.mediaRead()).mediaCreate(d.mediaCreate()).mediaUpdate(d.mediaUpdate())
                .mediaDelete(d.mediaDelete())
                .mediaTypeRead(d.mediaTypeRead()).mediaTypeCreate(d.mediaTypeCreate())
                .mediaTypeUpdate(d.mediaTypeUpdate()).mediaTypeDelete(d.mediaTypeDelete())
                .promotionRead(d.promotionRead()).promotionCreate(d.promotionCreate())
                .promotionUpdate(d.promotionUpdate()).promotionDelete(d.promotionDelete())
                .adviceRead(d.adviceRead()).adviceCreate(d.adviceCreate()).adviceUpdate(d.adviceUpdate())
                .adviceDelete(d.adviceDelete())
                .appVersionRead(d.appVersionRead()).appVersionCreate(d.appVersionCreate())
                .appVersionUpdate(d.appVersionUpdate()).appVersionDelete(d.appVersionDelete())
                .build();
    }
}
