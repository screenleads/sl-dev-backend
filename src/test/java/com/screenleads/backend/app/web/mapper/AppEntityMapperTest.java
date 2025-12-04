package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.EntityAttributeDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppEntityMapper Unit Tests")
class AppEntityMapperTest {

    @Test
    @DisplayName("toDto should convert AppEntity to AppEntityDTO with all fields")
    void whenToDtoEntity_thenConvertAllFields() {
        // Arrange
        AppEntity entity = new AppEntity();
        entity.setId(1L);
        entity.setResource("users");
        entity.setEntityName("User");
        entity.setClassName("com.example.User");
        entity.setTableName("users_table");
        entity.setIdType("Long");
        entity.setEndpointBase("/api/users");
        entity.setCreateLevel(Integer.valueOf(5));
        entity.setReadLevel(Integer.valueOf(1));
        entity.setUpdateLevel(Integer.valueOf(5));
        entity.setDeleteLevel(Integer.valueOf(10));
        entity.setVisibleInMenu(true);
        entity.setRowCount(100L);
        entity.setDisplayLabel("Users");
        entity.setIcon("user-icon");
        entity.setSortOrder(Integer.valueOf(1));

        // Act
        AppEntityDTO result = AppEntityMapper.toDto(entity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.resource()).isEqualTo("users");
        assertThat(result.entityName()).isEqualTo("User");
        assertThat(result.className()).isEqualTo("com.example.User");
        assertThat(result.tableName()).isEqualTo("users_table");
        assertThat(result.idType()).isEqualTo("Long");
        assertThat(result.endpointBase()).isEqualTo("/api/users");
        assertThat(result.createLevel()).isEqualTo(5);
        assertThat(result.readLevel()).isEqualTo(1);
        assertThat(result.updateLevel()).isEqualTo(5);
        assertThat(result.deleteLevel()).isEqualTo(10);
        assertThat(result.visibleInMenu()).isTrue();
        assertThat(result.rowCount()).isEqualTo(100);
        assertThat(result.displayLabel()).isEqualTo("Users");
        assertThat(result.icon()).isEqualTo("user-icon");
        assertThat(result.sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("toDto should convert AppEntity with attributes sorted by listOrder")
    void whenToDtoEntityWithAttributes_thenSortByListOrder() {
        // Arrange
        AppEntityAttribute attr1 = new AppEntityAttribute();
        attr1.setId(1L);
        attr1.setName("email");
        attr1.setListOrder(2);

        AppEntityAttribute attr2 = new AppEntityAttribute();
        attr2.setId(2L);
        attr2.setName("username");
        attr2.setListOrder(1);

        AppEntityAttribute attr3 = new AppEntityAttribute();
        attr3.setId(3L);
        attr3.setName("createdAt");
        attr3.setListOrder(3);

        AppEntity entity = new AppEntity();
        entity.setId(1L);
        entity.setResource("users");
        entity.setAttributes(Arrays.asList(attr1, attr2, attr3));

        // Act
        AppEntityDTO result = AppEntityMapper.toDto(entity);

        // Assert
        assertThat(result.attributes()).hasSize(3);
        assertThat(result.attributes().get(0).name()).isEqualTo("username");
        assertThat(result.attributes().get(1).name()).isEqualTo("email");
        assertThat(result.attributes().get(2).name()).isEqualTo("createdAt");
    }

    @Test
    @DisplayName("toDto should handle attributes with null listOrder")
    void whenToDtoEntityWithNullListOrder_thenPlaceAtEnd() {
        // Arrange
        AppEntityAttribute attr1 = new AppEntityAttribute();
        attr1.setId(1L);
        attr1.setName("sorted");
        attr1.setListOrder(1);

        AppEntityAttribute attr2 = new AppEntityAttribute();
        attr2.setId(2L);
        attr2.setName("unsorted");
        attr2.setListOrder(null);

        AppEntity entity = new AppEntity();
        entity.setId(1L);
        entity.setResource("test");
        entity.setAttributes(Arrays.asList(attr2, attr1));

        // Act
        AppEntityDTO result = AppEntityMapper.toDto(entity);

        // Assert
        assertThat(result.attributes()).hasSize(2);
        assertThat(result.attributes().get(0).name()).isEqualTo("sorted");
        assertThat(result.attributes().get(1).name()).isEqualTo("unsorted");
    }

    @Test
    @DisplayName("toDto should convert AppEntityAttribute to EntityAttributeDTO with all fields")
    void whenToDtoAttribute_thenConvertAllFields() {
        // Arrange
        AppEntityAttribute attr = new AppEntityAttribute();
        attr.setId(10L);
        attr.setName("status");
        attr.setAttrType("Enum");
        attr.setDataType("String");
        attr.setRelationTarget("StatusEnum");
        
        List<String> enumValues = Arrays.asList("ACTIVE", "INACTIVE", "PENDING");
        attr.setEnumValues(enumValues);
        
        attr.setListVisible(true);
        attr.setListOrder(5);
        attr.setListLabel("Status");
        attr.setListWidthPx(150);
        attr.setListAlign("center");
        attr.setListSearchable(true);
        attr.setListSortable(true);
        
        attr.setFormVisible(true);
        attr.setFormOrder(2);
        attr.setFormLabel("User Status");
        attr.setControlType("select");
        attr.setPlaceholder("Select status");
        attr.setHelpText("User account status");
        attr.setRequired(true);
        attr.setReadOnly(false);
        
        attr.setMinNum(null);
        attr.setMaxNum(null);
        attr.setMinLen(1);
        attr.setMaxLen(20);
        attr.setPattern("[A-Z]+");
        
        attr.setDefaultValue("PENDING");
        attr.setOptionsEndpoint("/api/statuses");

        // Act
        EntityAttributeDTO result = AppEntityMapper.toDto(attr);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("status");
        assertThat(result.attrType()).isEqualTo("Enum");
        assertThat(result.dataType()).isEqualTo("String");
        assertThat(result.relationTarget()).isEqualTo("StatusEnum");
        assertThat(result.enumValuesJson()).contains("ACTIVE", "INACTIVE", "PENDING");
        
        assertThat(result.listVisible()).isTrue();
        assertThat(result.listOrder()).isEqualTo(5);
        assertThat(result.listLabel()).isEqualTo("Status");
        assertThat(result.listWidthPx()).isEqualTo(150);
        assertThat(result.listAlign()).isEqualTo("center");
        assertThat(result.listSearchable()).isTrue();
        assertThat(result.listSortable()).isTrue();
        
        assertThat(result.formVisible()).isTrue();
        assertThat(result.formOrder()).isEqualTo(2);
        assertThat(result.formLabel()).isEqualTo("User Status");
        assertThat(result.controlType()).isEqualTo("select");
        assertThat(result.placeholder()).isEqualTo("Select status");
        assertThat(result.helpText()).isEqualTo("User account status");
        assertThat(result.required()).isTrue();
        assertThat(result.readOnly()).isFalse();
        
        assertThat(result.minLen()).isEqualTo(1);
        assertThat(result.maxLen()).isEqualTo(20);
        assertThat(result.pattern()).isEqualTo("[A-Z]+");
        
        assertThat(result.defaultValue()).isEqualTo("PENDING");
        assertThat(result.optionsEndpoint()).isEqualTo("/api/statuses");
    }

    @Test
    @DisplayName("toDto should handle attribute with null enumValues")
    void whenToDtoAttributeWithNullEnumValues_thenEnumJsonIsNull() {
        // Arrange
        AppEntityAttribute attr = new AppEntityAttribute();
        attr.setId(1L);
        attr.setName("name");
        attr.setEnumValues(null);

        // Act
        EntityAttributeDTO result = AppEntityMapper.toDto(attr);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.enumValuesJson()).isNull();
    }

    @Test
    @DisplayName("toDto should handle entity with null attributes list")
    void whenToDtoEntityWithNullAttributes_thenAttributesIsEmpty() {
        // Arrange
        AppEntity entity = new AppEntity();
        entity.setId(1L);
        entity.setResource("test");
        entity.setAttributes(null);

        // Act
        AppEntityDTO result = AppEntityMapper.toDto(entity);

        // Assert
        assertThat(result.attributes()).isEmpty();
    }

    @Test
    @DisplayName("toDto should handle entity with empty attributes list")
    void whenToDtoEntityWithEmptyAttributes_thenAttributesIsEmpty() {
        // Arrange
        AppEntity entity = new AppEntity();
        entity.setId(1L);
        entity.setResource("test");
        entity.setAttributes(new ArrayList<>());

        // Act
        AppEntityDTO result = AppEntityMapper.toDto(entity);

        // Assert
        assertThat(result.attributes()).isEmpty();
    }

    @Test
    @DisplayName("applyAttrDto should apply non-null values to attribute")
    void whenApplyAttrDto_thenUpdateNonNullFields() {
        // Arrange
        AppEntityAttribute attr = new AppEntityAttribute();
        attr.setId(1L);
        attr.setName("oldName");
        attr.setListOrder(1);
        attr.setFormLabel("Old Label");

        EntityAttributeDTO dto = EntityAttributeDTO.builder()
                .name("newName")
                .listOrder(5)
                .formLabel("New Label")
                .required(true)
                .build();

        // Act
        AppEntityMapper.applyAttrDto(attr, dto);

        // Assert
        assertThat(attr.getName()).isEqualTo("newName");
        assertThat(attr.getListOrder()).isEqualTo(5);
        assertThat(attr.getFormLabel()).isEqualTo("New Label");
        assertThat(attr.getRequired()).isTrue();
    }

    @Test
    @DisplayName("applyAttrDto should not modify fields when DTO values are null")
    void whenApplyAttrDtoWithNullValues_thenKeepOriginalValues() {
        // Arrange
        AppEntityAttribute attr = new AppEntityAttribute();
        attr.setId(1L);
        attr.setName("originalName");
        attr.setListOrder(3);

        EntityAttributeDTO dto = EntityAttributeDTO.builder()
                .name(null)
                .listOrder(null)
                .build();

        // Act
        AppEntityMapper.applyAttrDto(attr, dto);

        // Assert
        assertThat(attr.getName()).isEqualTo("originalName");
        assertThat(attr.getListOrder()).isEqualTo(3);
    }
}
