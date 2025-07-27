package com.screenleads.backend.app.web.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersionDTO {

    private Long id;
    private String platform;
    private String version;
    private String message;
    private String url;
    private boolean forceUpdate;
}