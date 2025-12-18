package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaTypeServiceImpl Unit Tests")
class MediaTypeServiceImplTest {

    @Mock
    private MediaTypeRepository mediaTypeRepository;

    @InjectMocks
    private MediaTypeServiceImpl mediaTypeService;

    private MediaType testMediaType;

    @BeforeEach
    void setUp() {
        testMediaType = new MediaType();
        testMediaType.setId(1L);
        testMediaType.setType("IMAGE");
        testMediaType.setExtension("jpg");
        testMediaType.setEnabled(true);

        testMediaTypeDTO = new MediaTypeDTO(1L, "jpg", "IMAGE", true);
    }

    @Test
    @DisplayName("getAllMediaTypes should return all media types sorted by id")
    void whenGetAllMediaTypes_thenReturnsAllSorted() {
        // Arrange
        MediaType type2 = new MediaType();
        type2.setId(3L);
        type2.setType("VIDEO");
        type2.setExtension("mp4");
        type2.setEnabled(true);

        MediaType type3 = new MediaType();
        type3.setId(2L);
        type3.setType("AUDIO");
        type3.setExtension("mp3");
        type3.setEnabled(true);

        when(mediaTypeRepository.findAll()).thenReturn(Arrays.asList(type2, testMediaType, type3));

        // Act
        List<MediaTypeDTO> result = mediaTypeService.getAllMediaTypes();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(2).id()).isEqualTo(3L);
        verify(mediaTypeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllMediaTypes should return empty list when no types exist")
    void whenGetAllMediaTypesEmpty_thenReturnsEmptyList() {
        // Arrange
        when(mediaTypeRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<MediaTypeDTO> result = mediaTypeService.getAllMediaTypes();

        // Assert
        assertThat(result).isEmpty();
        verify(mediaTypeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getMediaTypeById should return media type when found")
    void whenGetMediaTypeByIdExists_thenReturnsMediaType() {
        // Arrange
        when(mediaTypeRepository.findById(1L)).thenReturn(Optional.of(testMediaType));

        // Act
        Optional<MediaTypeDTO> result = mediaTypeService.getMediaTypeById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().type()).isEqualTo("IMAGE");
        assertThat(result.get().extension()).isEqualTo("jpg");
        verify(mediaTypeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getMediaTypeById should return empty when not found")
    void whenGetMediaTypeByIdNotExists_thenReturnsEmpty() {
        // Arrange
        when(mediaTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<MediaTypeDTO> result = mediaTypeService.getMediaTypeById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(mediaTypeRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("saveMediaType should create new media type when extension and type don't exist")
    void whenSaveMediaTypeNew_thenCreatesNew() {
        // Arrange
        MediaTypeDTO inputDTO = new MediaTypeDTO(null, "png", "IMAGE", true);
        MediaType savedType = new MediaType();
        savedType.setId(10L);
        savedType.setExtension("png");
        savedType.setType("IMAGE");
        savedType.setEnabled(true);

        when(mediaTypeRepository.findByExtension("png")).thenReturn(Optional.empty());
        when(mediaTypeRepository.findByType("IMAGE")).thenReturn(Optional.empty());
        when(mediaTypeRepository.save(any(MediaType.class))).thenReturn(savedType);

        // Act
        MediaTypeDTO result = mediaTypeService.saveMediaType(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.extension()).isEqualTo("png");
        verify(mediaTypeRepository, times(1)).findByExtension("png");
        verify(mediaTypeRepository, times(1)).findByType("IMAGE");
        verify(mediaTypeRepository, times(1)).save(any(MediaType.class));
    }

    @Test
    @DisplayName("saveMediaType should return existing when extension already exists")
    void whenSaveMediaTypeWithExistingExtension_thenReturnsExisting() {
        // Arrange
        MediaTypeDTO inputDTO = new MediaTypeDTO(null, "jpg", "IMAGE", true);

        when(mediaTypeRepository.findByExtension("jpg")).thenReturn(Optional.of(testMediaType));

        // Act
        MediaTypeDTO result = mediaTypeService.saveMediaType(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.extension()).isEqualTo("jpg");
        verify(mediaTypeRepository, times(1)).findByExtension("jpg");
        verify(mediaTypeRepository, never()).save(any(MediaType.class));
    }

    @Test
    @DisplayName("saveMediaType should return existing when type already exists")
    void whenSaveMediaTypeWithExistingType_thenReturnsExisting() {
        // Arrange
        MediaTypeDTO inputDTO = new MediaTypeDTO(null, "jpeg", "IMAGE", true);

        when(mediaTypeRepository.findByExtension("jpeg")).thenReturn(Optional.empty());
        when(mediaTypeRepository.findByType("IMAGE")).thenReturn(Optional.of(testMediaType));

        // Act
        MediaTypeDTO result = mediaTypeService.saveMediaType(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo("IMAGE");
        verify(mediaTypeRepository, times(1)).findByExtension("jpeg");
        verify(mediaTypeRepository, times(1)).findByType("IMAGE");
        verify(mediaTypeRepository, never()).save(any(MediaType.class));
    }

    @Test
    @DisplayName("updateMediaType should update media type successfully")
    void whenUpdateMediaTypeExists_thenUpdatesSuccessfully() {
        // Arrange
        MediaTypeDTO updateDTO = new MediaTypeDTO(1L, "jpeg", "IMAGE", false);
        MediaType updatedType = new MediaType();
        updatedType.setId(1L);
        updatedType.setExtension("jpeg");
        updatedType.setType("IMAGE");
        updatedType.setEnabled(false);

        when(mediaTypeRepository.findById(1L)).thenReturn(Optional.of(testMediaType));
        when(mediaTypeRepository.save(any(MediaType.class))).thenReturn(updatedType);

        // Act
        MediaTypeDTO result = mediaTypeService.updateMediaType(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.extension()).isEqualTo("jpeg");
        assertThat(result.enabled()).isFalse();
        verify(mediaTypeRepository, times(1)).findById(1L);
        verify(mediaTypeRepository, times(1)).save(any(MediaType.class));
    }

    @Test
    @DisplayName("updateMediaType should throw exception when media type not found")
    void whenUpdateMediaTypeNotExists_thenThrowsException() {
        // Arrange
        MediaTypeDTO updateDTO = new MediaTypeDTO(999L, "jpg", "IMAGE", true);
        when(mediaTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> mediaTypeService.updateMediaType(999L, updateDTO))
                .isInstanceOf(NoSuchElementException.class);

        verify(mediaTypeRepository, times(1)).findById(999L);
        verify(mediaTypeRepository, never()).save(any(MediaType.class));
    }

    @Test
    @DisplayName("deleteMediaType should delete media type by id")
    void whenDeleteMediaType_thenDeletesSuccessfully() {
        // Act
        mediaTypeService.deleteMediaType(1L);

        // Assert
        verify(mediaTypeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteMediaType should call repository even if type doesn't exist")
    void whenDeleteNonExistentMediaType_thenCallsRepository() {
        // Act
        mediaTypeService.deleteMediaType(999L);

        // Assert
        verify(mediaTypeRepository, times(1)).deleteById(999L);
    }
}
