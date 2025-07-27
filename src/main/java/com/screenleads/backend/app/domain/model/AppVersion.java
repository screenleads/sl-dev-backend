package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String platform; // "android" o "ios"
    private String version;
    private String message;
    private String url;

    private boolean forceUpdate;
}
