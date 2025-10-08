// AppEntityAttribute.java
package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_entity_attribute", uniqueConstraints = @UniqueConstraint(columnNames = { "app_entity_id",
        "attr_name" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppEntityAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_entity_id")
    private AppEntity appEntity;

    @Column(name = "attr_name", nullable = false, length = 100)
    private String name; // clave (ej: "name", "primaryColor")

    @Column(name = "attr_type", length = 80)
    private String attrType; // compat con lo que tenías (String, Long, Boolean...)

    // Metadatos extendidos
    @Column(name = "data_type", length = 50)
    private String dataType; // opcional si quieres diferenciarlo de attrType

    @Column(name = "relation_target", length = 80)
    private String relationTarget; // ej: "Company", "Media"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enum_values", columnDefinition = "jsonb")
    private List<String> enumValues; // o JsonNode / List<Map<String,Object>>

    // LIST
    @Column(name = "list_visible")
    @Builder.Default
    private Boolean listVisible = Boolean.TRUE;

    @Column(name = "list_order")
    private Integer listOrder;

    @Column(name = "list_label", length = 150)
    private String listLabel;

    @Column(name = "list_width_px")
    private Integer listWidthPx;

    @Column(name = "list_align", length = 10)
    private String listAlign;

    @Column(name = "list_searchable")
    @Builder.Default
    private Boolean listSearchable = Boolean.TRUE;

    @Column(name = "list_sortable")
    @Builder.Default
    private Boolean listSortable = Boolean.TRUE;

    // FORM
    @Column(name = "form_visible")
    @Builder.Default
    private Boolean formVisible = Boolean.TRUE;

    @Column(name = "form_order")
    private Integer formOrder;

    @Column(name = "form_label", length = 150)
    private String formLabel;

    @Column(name = "control_type", length = 30)
    private String controlType;

    @Column(name = "placeholder", length = 200)
    private String placeholder;

    @Column(name = "help_text", length = 300)
    private String helpText;

    @Column(name = "required")
    @Builder.Default
    private Boolean required = Boolean.FALSE;

    @Column(name = "read_only")
    @Builder.Default
    private Boolean readOnly = Boolean.FALSE;

    @Column(name = "min_num", precision = 18, scale = 6)
    private BigDecimal minNum;

    @Column(name = "max_num", precision = 18, scale = 6)
    private BigDecimal maxNum;

    @Column(name = "min_len")
    private Integer minLen;

    @Column(name = "max_len")
    private Integer maxLen;

    @Column(name = "pattern", length = 200)
    private String pattern;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "options_endpoint", length = 200)
    private String optionsEndpoint;
}
