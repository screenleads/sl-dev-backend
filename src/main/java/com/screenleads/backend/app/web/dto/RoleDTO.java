// com.screenleads.backend.app.web.dto.RoleDTO
package com.screenleads.backend.app.web.dto;

public record RoleDTO(
                Long id,
                String role,
                String description,
                Integer level,

                boolean userRead, boolean userCreate, boolean userUpdate, boolean userDelete,
                boolean companyRead, boolean companyCreate, boolean companyUpdate, boolean companyDelete,
                boolean deviceRead, boolean deviceCreate, boolean deviceUpdate, boolean deviceDelete,
                boolean deviceTypeRead, boolean deviceTypeCreate, boolean deviceTypeUpdate, boolean deviceTypeDelete,
                boolean mediaRead, boolean mediaCreate, boolean mediaUpdate, boolean mediaDelete,
                boolean mediaTypeRead, boolean mediaTypeCreate, boolean mediaTypeUpdate, boolean mediaTypeDelete,
                boolean promotionRead, boolean promotionCreate, boolean promotionUpdate, boolean promotionDelete,
                boolean adviceRead, boolean adviceCreate, boolean adviceUpdate, boolean adviceDelete,
                boolean appVersionRead, boolean appVersionCreate, boolean appVersionUpdate, boolean appVersionDelete) {
}
