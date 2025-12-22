package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.EntityAttributeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppEntityServiceImpl Unit Tests")
class AppEntityServiceImplTest {

    @Mock
    private AppEntityRepository appEntityRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AppEntityServiceImpl appEntityService;

    private AppEntity testEntity;
    private AppEntityDTO testEntityDTO;
    private AppEntityAttribute testAttribute;

    @BeforeEach
    void setUp() {
        // Initialize JdbcTemplate for count queries
        appEntityService.setJdbcTemplate(jdbcTemplate);

        // Setup test attribute
        testAttribute = new AppEntityAttribute();
        testAttribute.setId(1L);
        testAttribute.setName("testAttr");
        testAttribute.setListOrder(1);
        testAttribute.setFormOrder(1);

        // Setup test entity
        List<AppEntityAttribute> attributes = new ArrayList<>();
        attributes.add(testAttribute);

        testEntity = AppEntity.builder()
                .id(1L)
                .resource("test_resource")
                .entityName("TestEntity")
                .className("com.test.TestEntity")
                .tableName("test_table")
                .idType("Long")
                .endpointBase("/api/test")
                .createLevel(1)
                .readLevel(1)
                .updateLevel(1)
                .deleteLevel(1)
                .visibleInMenu(true)
                .displayLabel("Test Entity")
                .icon("test-icon")
                .sortOrder(1)
                .attributes(attributes)
                .build();
        testAttribute.setAppEntity(testEntity);

        // Setup test DTO
        testEntityDTO = AppEntityDTO.builder()
                .id(1L)
                .resource("test_resource")
                .entityName("TestEntity")
                .className("com.test.TestEntity")
                .tableName("test_table")
                .idType("Long")
                .endpointBase("/api/test")
                .createLevel(1)
                .readLevel(1)
                .updateLevel(1)
                .deleteLevel(1)
                .visibleInMenu(true)
                .displayLabel("Test Entity")
                .icon("test-icon")
                .sortOrder(1)
                .rowCount(null)
                .attributes(List.of(
                        EntityAttributeDTO.builder()
                                .id(1L)
                                .name("testAttr")
                                .listOrder(1)
                                .formOrder(1)
                                .build()))
                .build();
    }

    // ===================== QUERY TESTS =====================

    @Test
    @DisplayName("findAll should return all entities without count when withCount is false")
    void whenFindAllWithoutCount_thenReturnAllEntities() {
        // Arrange
        when(appEntityRepository.findAll()).thenReturn(new ArrayList<>(List.of(testEntity)));

        // Act
        List<AppEntityDTO> result = appEntityService.findAll(false);

        // Assert
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).resource()).isEqualTo("test_resource");
        verify(appEntityRepository, times(1)).findAll();
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    @DisplayName("findAll should return entities with row count when withCount is true")
    void whenFindAllWithCount_thenReturnEntitiesWithCount() {
        // Arrange
        when(appEntityRepository.findAll()).thenReturn(new ArrayList<>(List.of(testEntity)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(42L);

        // Act
        List<AppEntityDTO> result = appEntityService.findAll(true);

        // Assert
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).rowCount()).isEqualTo(42L);
        verify(appEntityRepository, times(1)).findAll();
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    @DisplayName("findById should return entity when found")
    void whenFindById_thenReturnEntity() {
        // Arrange
        when(appEntityRepository.findById(1L)).thenReturn(Optional.of(testEntity));

        // Act
        AppEntityDTO result = appEntityService.findById(1L, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.resource()).isEqualTo("test_resource");
        verify(appEntityRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById should throw exception when entity not found")
    void whenFindByIdNotFound_thenThrowException() {
        // Arrange
        when(appEntityRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appEntityService.findById(999L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AppEntity no encontrada");
        verify(appEntityRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("findByResource should return entity when found")
    void whenFindByResource_thenReturnEntity() {
        // Arrange
        when(appEntityRepository.findByResource("test_resource")).thenReturn(Optional.of(testEntity));

        // Act
        AppEntityDTO result = appEntityService.findByResource("test_resource", false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.resource()).isEqualTo("test_resource");
        verify(appEntityRepository, times(1)).findByResource("test_resource");
    }

    @Test
    @DisplayName("findByResource should throw exception when entity not found")
    void whenFindByResourceNotFound_thenThrowException() {
        // Arrange
        when(appEntityRepository.findByResource("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appEntityService.findByResource("unknown", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AppEntity no encontrada");
        verify(appEntityRepository, times(1)).findByResource("unknown");
    }

    // ===================== COMMAND TESTS =====================

    @Test
    @DisplayName("upsert should create new entity when id is null")
    void whenUpsertWithNullId_thenCreateNewEntity() {
        // Arrange
        AppEntityDTO newDto = testEntityDTO.toBuilder().id(null).build();
        when(appEntityRepository.findByResource("test_resource")).thenReturn(Optional.empty());
        when(appEntityRepository.save(any(AppEntity.class))).thenReturn(testEntity);

        // Act
        AppEntityDTO result = appEntityService.upsert(newDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.resource()).isEqualTo("test_resource");
        verify(appEntityRepository, times(1)).findByResource("test_resource");
        verify(appEntityRepository, times(1)).save(any(AppEntity.class));
    }

    @Test
    @DisplayName("upsert should update existing entity when id is provided")
    void whenUpsertWithId_thenUpdateEntity() {
        // Arrange
        when(appEntityRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(appEntityRepository.save(any(AppEntity.class))).thenReturn(testEntity);

        // Act
        AppEntityDTO result = appEntityService.upsert(testEntityDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(appEntityRepository, times(1)).findById(1L);
        verify(appEntityRepository, times(1)).save(any(AppEntity.class));
    }

    @Test
    @DisplayName("upsert should throw exception when DTO is null")
    void whenUpsertWithNullDto_thenThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> appEntityService.upsert(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AppEntityDTO nulo");
    }

    @Test
    @DisplayName("upsert should throw exception when resource is blank")
    void whenUpsertWithBlankResource_thenThrowException() {
        // Arrange
        AppEntityDTO invalidDto = testEntityDTO.toBuilder().resource("").build();

        // Act & Assert
        assertThatThrownBy(() -> appEntityService.upsert(invalidDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resource obligatorio");
    }

    @Test
    @DisplayName("deleteById should delete entity")
    void whenDeleteById_thenEntityIsDeleted() {
        // Arrange
        doNothing().when(appEntityRepository).deleteById(1L);

        // Act
        appEntityService.deleteById(1L);

        // Assert
        verify(appEntityRepository, times(1)).deleteById(1L);
    }

    // ===================== REORDER TESTS =====================

    @Test
    @DisplayName("reorderEntities should reorder visible entities")
    void whenReorderEntities_thenOrderIsUpdated() {
        // Arrange
        AppEntity entity1 = testEntity;
        entity1.setSortOrder(1);
        AppEntity entity2 = AppEntity.builder()
                .id(2L)
                .resource("resource2")
                .visibleInMenu(true)
                .sortOrder(2)
                .build();

        when(appEntityRepository.findByVisibleInMenuTrueOrderBySortOrderAsc())
                .thenReturn(Arrays.asList(entity1, entity2));
        when(appEntityRepository.saveAll(anyList())).thenReturn(Arrays.asList(entity2, entity1));

        // Act
        appEntityService.reorderEntities(Arrays.asList(2L, 1L)); // Swap order

        // Assert
        assertThat(entity2.getSortOrder()).isEqualTo(1);
        assertThat(entity1.getSortOrder()).isEqualTo(2);
        verify(appEntityRepository, times(1)).findByVisibleInMenuTrueOrderBySortOrderAsc();
        verify(appEntityRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("reorderEntities should throw exception when list is empty")
    void whenReorderEntitiesWithEmptyList_thenThrowException() {
        // Act & Assert
        assertThatThrownBy(this::reorderWithEmptyList)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Debe enviarse una lista de IDs");
    }

    private void reorderWithEmptyList() {
        appEntityService.reorderEntities(List.of());
    }

    @Test
    @DisplayName("reorderEntities should throw exception when list contains duplicates")
    void whenReorderEntitiesWithDuplicates_thenThrowException() {
        // Act & Assert
        assertThatThrownBy(this::reorderWithDuplicates)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicados");
    }

    private void reorderWithDuplicates() {
        appEntityService.reorderEntities(Arrays.asList(1L, 1L));
    }

    @Test
    @DisplayName("reorderEntities should throw exception when ID not in visible entities")
    void whenReorderEntitiesWithInvalidId_thenThrowException() {
        // Arrange
        when(appEntityRepository.findByVisibleInMenuTrueOrderBySortOrderAsc())
                .thenReturn(List.of(testEntity));

        // Act & Assert
        assertThatThrownBy(this::reorderWithInvalidId)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece a entidades visibles");
    }

    private void reorderWithInvalidId() {
        appEntityService.reorderEntities(Arrays.asList(999L));
    }

    @Test
    @DisplayName("reorderAttributes should reorder entity attributes")
    void whenReorderAttributes_thenAttributeOrderIsUpdated() {
        // Arrange
        AppEntityAttribute attr1 = testAttribute;
        attr1.setListOrder(1);
        AppEntityAttribute attr2 = new AppEntityAttribute();
        attr2.setId(2L);
        attr2.setListOrder(2);
        attr2.setAppEntity(testEntity);
        testEntity.setAttributes(Arrays.asList(attr1, attr2));

        when(appEntityRepository.findWithAttributesById(1L)).thenReturn(Optional.of(testEntity));
        when(appEntityRepository.save(any(AppEntity.class))).thenReturn(testEntity);

        // Act
        appEntityService.reorderAttributes(1L, Arrays.asList(2L, 1L)); // Swap order

        // Assert
        assertThat(attr2.getListOrder()).isEqualTo(1);
        assertThat(attr1.getListOrder()).isEqualTo(2);
        verify(appEntityRepository, times(1)).findWithAttributesById(1L);
        verify(appEntityRepository, times(1)).save(any(AppEntity.class));
    }

    @Test
    @DisplayName("reorderAttributes should throw exception when entity not found")
    void whenReorderAttributesEntityNotFound_thenThrowException() {
        // Arrange
        when(appEntityRepository.findWithAttributesById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(this::reorderAttributesNotFound)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AppEntity no encontrada");
    }

    private void reorderAttributesNotFound() {
        appEntityService.reorderAttributes(999L, List.of(1L));
    }

    @Test
    @DisplayName("reorderAttributes should throw exception when attribute list contains duplicates")
    void whenReorderAttributesWithDuplicates_thenThrowException() {
        // Act & Assert
        assertThatThrownBy(this::reorderAttributesWithDuplicates)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicados");
    }

    private void reorderAttributesWithDuplicates() {
        appEntityService.reorderAttributes(1L, Arrays.asList(1L, 1L));
    }
}