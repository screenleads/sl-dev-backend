package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "role",
  uniqueConstraints = @UniqueConstraint(name = "uk_role_name", columnNames = "role")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="role", nullable=false, length=50)
  private String role;

  @Column(name="description", length=255)
  private String description;

  @Builder.Default
  @Column(name="level", nullable=false)
  private Integer level = 99;

  // ==== FLAGS CRUD (booleans) ====
  @Builder.Default @Column(nullable=false) private boolean userRead=false;
  @Builder.Default @Column(nullable=false) private boolean userCreate=false;
  @Builder.Default @Column(nullable=false) private boolean userUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean userDelete=false;

  @Builder.Default @Column(nullable=false) private boolean companyRead=false;
  @Builder.Default @Column(nullable=false) private boolean companyCreate=false;
  @Builder.Default @Column(nullable=false) private boolean companyUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean companyDelete=false;

  @Builder.Default @Column(nullable=false) private boolean deviceRead=false;
  @Builder.Default @Column(nullable=false) private boolean deviceCreate=false;
  @Builder.Default @Column(nullable=false) private boolean deviceUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean deviceDelete=false;

  @Builder.Default @Column(nullable=false) private boolean deviceTypeRead=false;
  @Builder.Default @Column(nullable=false) private boolean deviceTypeCreate=false;
  @Builder.Default @Column(nullable=false) private boolean deviceTypeUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean deviceTypeDelete=false;

  @Builder.Default @Column(nullable=false) private boolean mediaRead=false;
  @Builder.Default @Column(nullable=false) private boolean mediaCreate=false;
  @Builder.Default @Column(nullable=false) private boolean mediaUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean mediaDelete=false;

  @Builder.Default @Column(nullable=false) private boolean mediaTypeRead=false;
  @Builder.Default @Column(nullable=false) private boolean mediaTypeCreate=false;
  @Builder.Default @Column(nullable=false) private boolean mediaTypeUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean mediaTypeDelete=false;

  @Builder.Default @Column(nullable=false) private boolean promotionRead=false;
  @Builder.Default @Column(nullable=false) private boolean promotionCreate=false;
  @Builder.Default @Column(nullable=false) private boolean promotionUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean promotionDelete=false;

  @Builder.Default @Column(nullable=false) private boolean adviceRead=false;
  @Builder.Default @Column(nullable=false) private boolean adviceCreate=false;
  @Builder.Default @Column(nullable=false) private boolean adviceUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean adviceDelete=false;

  @Builder.Default @Column(nullable=false) private boolean appVersionRead=false;
  @Builder.Default @Column(nullable=false) private boolean appVersionCreate=false;
  @Builder.Default @Column(nullable=false) private boolean appVersionUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean appVersionDelete=false;

  @Builder.Default @Column(nullable=false) private boolean roleRead=false;
  @Builder.Default @Column(nullable=false) private boolean roleCreate=false;
  @Builder.Default @Column(nullable=false) private boolean roleUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean roleDelete=false;

  @Builder.Default @Column(nullable=false) private boolean promotionLeadRead=false;
  @Builder.Default @Column(nullable=false) private boolean promotionLeadCreate=false;
  @Builder.Default @Column(nullable=false) private boolean promotionLeadUpdate=false;
  @Builder.Default @Column(nullable=false) private boolean promotionLeadDelete=false;

  public Set<String> toAuthorities() {
    Set<String> auth = new HashSet<>();
    if (role != null && !role.isBlank()) {
      auth.add(role.startsWith("ROLE_") ? role : "ROLE_" + role);
    }
    java.util.function.BiConsumer<Boolean,String> add = (b,n) -> { if (b) auth.add(n); };
    add.accept(userRead,"USER_READ"); add.accept(userCreate,"USER_CREATE"); add.accept(userUpdate,"USER_UPDATE"); add.accept(userDelete,"USER_DELETE");
    add.accept(companyRead,"COMPANY_READ"); add.accept(companyCreate,"COMPANY_CREATE"); add.accept(companyUpdate,"COMPANY_UPDATE"); add.accept(companyDelete,"COMPANY_DELETE");
    add.accept(deviceRead,"DEVICE_READ"); add.accept(deviceCreate,"DEVICE_CREATE"); add.accept(deviceUpdate,"DEVICE_UPDATE"); add.accept(deviceDelete,"DEVICE_DELETE");
    add.accept(deviceTypeRead,"DEVICETYPE_READ"); add.accept(deviceTypeCreate,"DEVICETYPE_CREATE"); add.accept(deviceTypeUpdate,"DEVICETYPE_UPDATE"); add.accept(deviceTypeDelete,"DEVICETYPE_DELETE");
    add.accept(mediaRead,"MEDIA_READ"); add.accept(mediaCreate,"MEDIA_CREATE"); add.accept(mediaUpdate,"MEDIA_UPDATE"); add.accept(mediaDelete,"MEDIA_DELETE");
    add.accept(mediaTypeRead,"MEDIATYPE_READ"); add.accept(mediaTypeCreate,"MEDIATYPE_CREATE"); add.accept(mediaTypeUpdate,"MEDIATYPE_UPDATE"); add.accept(mediaTypeDelete,"MEDIATYPE_DELETE");
    add.accept(promotionRead,"PROMOTION_READ"); add.accept(promotionCreate,"PROMOTION_CREATE"); add.accept(promotionUpdate,"PROMOTION_UPDATE"); add.accept(promotionDelete,"PROMOTION_DELETE");
    add.accept(adviceRead,"ADVICE_READ"); add.accept(adviceCreate,"ADVICE_CREATE"); add.accept(adviceUpdate,"ADVICE_UPDATE"); add.accept(adviceDelete,"ADVICE_DELETE");
    add.accept(appVersionRead,"APPVERSION_READ"); add.accept(appVersionCreate,"APPVERSION_CREATE"); add.accept(appVersionUpdate,"APPVERSION_UPDATE"); add.accept(appVersionDelete,"APPVERSION_DELETE");
    add.accept(roleRead,"ROLE_READ"); add.accept(roleCreate,"ROLE_CREATE"); add.accept(roleUpdate,"ROLE_UPDATE"); add.accept(roleDelete,"ROLE_DELETE");
    add.accept(promotionLeadRead,"PROMOTIONLEAD_READ"); add.accept(promotionLeadCreate,"PROMOTIONLEAD_CREATE"); add.accept(promotionLeadUpdate,"PROMOTIONLEAD_UPDATE"); add.accept(promotionLeadDelete,"PROMOTIONLEAD_DELETE");
    return auth;
  }
}
