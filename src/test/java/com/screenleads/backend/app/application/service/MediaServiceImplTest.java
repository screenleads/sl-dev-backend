package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.MediaDTO;

/**
 * Unit tests for MediaServiceImpl.
 * 
 * Testing:
 * - CRUD operations for media entities
 * - DTO/Entity conversion
 * - Media type validation
 * - Sorting and ordering
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaServiceImpl Unit Tests")
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaTypeRepository mediaTypeRepository;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private MediaType testMediaType;
    private Media testMedia1;
    private Media testMedia2;

    @BeforeEach
    void setUp() {
        // Setup media type
        testMediaType = new MediaType();
        testMediaType.setId(1L);
        testMediaType.setType("VIDEO");
        testMediaType.setExtension("mp4");

        // Setup media entities
        testMedia1 = createMedia(1L, "https://example.com/video1.mp4", testMediaType);
        testMedia2 = createMedia(2L, "https://example.com/video2.mp4", testMediaType);
    }

    @Nested
    @DisplayName("Get All Media Tests")
    class GetAllMediaTests {

        @Test
        @DisplayName("Should return all media sorted by ID")
        void whenGetAllMedias_thenReturnSortedList() {
            // Given
            when(mediaRepository.findAll()).thenReturn(Arrays.asList(testMedia2, testMedia1));

            // When
            List<MediaDTO> result = mediaService.getAllMedias();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(1).id()).isEqualTo(2L);
            verify(mediaRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no media exists")
        void whenGetAllMedias_withNoMedia_thenReturnEmptyList() {
            // Given
            when(mediaRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MediaDTO> result = mediaService.getAllMedias();

            // Then
            assertThat(result).isEmpty();
            verify(mediaRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Get Media By ID Tests")
    class GetMediaByIdTests {

        @Test
        @DisplayName("Should return media when ID exists")
        void whenGetMediaById_withValidId_thenReturnMedia() {
            // Given
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia1));

            // When
            Optional<MediaDTO> result = mediaService.getMediaById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            assertThat(result.get().src()).isEqualTo("https://example.com/video1.mp4");
            verify(mediaRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when ID does not exist")
        void whenGetMediaById_withInvalidId_thenReturnEmpty() {
            // Given
            when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<MediaDTO> result = mediaService.getMediaById(999L);

            // Then
            assertThat(result).isEmpty();
            verify(mediaRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Save Media Tests")
    class SaveMediaTests {

        @Test
        @DisplayName("Should save media with valid data")
        void whenSaveMedia_withValidData_thenReturnSavedMedia() {
            // Given
            MediaDTO inputDTO = new MediaDTO(null, "https://example.com/new.mp4", testMediaType);
            Media newMedia = createMedia(null, "https://example.com/new.mp4", testMediaType);
            Media savedMedia = createMedia(3L, "https://example.com/new.mp4", testMediaType);

            when(mediaTypeRepository.findById(1L)).thenReturn(Optional.of(testMediaType));
            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

            // When
            MediaDTO result = mediaService.saveMedia(inputDTO);

            // Then
            assertThat(result.id()).isEqualTo(3L);
            assertThat(result.src()).isEqualTo("https://example.com/new.mp4");
            verify(mediaTypeRepository).findById(1L);
            verify(mediaRepository).save(any(Media.class));
        }

        @Test
        @DisplayName("Should throw exception when media type is null")
        void whenSaveMedia_withNullMediaType_thenThrowException() {
            // Given
            MediaDTO inputDTO = new MediaDTO(null, "https://example.com/new.mp4", null);

            // When/Then
            assertThatThrownBy(() -> mediaService.saveMedia(inputDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Media type requerido");
            
            verify(mediaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when media type ID is null")
        void whenSaveMedia_withNullMediaTypeId_thenThrowException() {
            // Given
            MediaType typeWithoutId = new MediaType();
            MediaDTO inputDTO = new MediaDTO(null, "https://example.com/new.mp4", typeWithoutId);

            // When/Then
            assertThatThrownBy(() -> mediaService.saveMedia(inputDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Media type requerido");
            
            verify(mediaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when media type not found")
        void whenSaveMedia_withNonexistentMediaType_thenThrowException() {
            // Given
            MediaDTO inputDTO = new MediaDTO(null, "https://example.com/new.mp4", testMediaType);
            when(mediaTypeRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> mediaService.saveMedia(inputDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Media type no encontrado");
            
            verify(mediaTypeRepository).findById(1L);
            verify(mediaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Media Tests")
    class UpdateMediaTests {

        @Test
        @DisplayName("Should update media with valid data")
        void whenUpdateMedia_withValidData_thenReturnUpdatedMedia() {
            // Given
            MediaDTO updateDTO = new MediaDTO(1L, "https://example.com/updated.mp4", testMediaType);
            Media updatedMedia = createMedia(1L, "https://example.com/updated.mp4", testMediaType);

            when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia1));
            when(mediaRepository.save(any(Media.class))).thenReturn(updatedMedia);

            // When
            MediaDTO result = mediaService.updateMedia(1L, updateDTO);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.src()).isEqualTo("https://example.com/updated.mp4");
            verify(mediaRepository).findById(1L);
            verify(mediaRepository).save(any(Media.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent media")
        void whenUpdateMedia_withInvalidId_thenThrowException() {
            // Given
            MediaDTO updateDTO = new MediaDTO(999L, "https://example.com/updated.mp4", testMediaType);
            when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> mediaService.updateMedia(999L, updateDTO))
                    .isInstanceOf(NoSuchElementException.class);
            
            verify(mediaRepository).findById(999L);
            verify(mediaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Media Tests")
    class DeleteMediaTests {

        @Test
        @DisplayName("Should delete media by ID")
        void whenDeleteMedia_withValidId_thenMediaIsDeleted() {
            // Given
            doNothing().when(mediaRepository).deleteById(1L);

            // When
            mediaService.deleteMedia(1L);

            // Then
            verify(mediaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should handle deletion of non-existent media")
        void whenDeleteMedia_withInvalidId_thenNoException() {
            // Given
            doNothing().when(mediaRepository).deleteById(999L);

            // When
            mediaService.deleteMedia(999L);

            // Then
            verify(mediaRepository).deleteById(999L);
        }
    }

    // Helper methods
    private Media createMedia(Long id, String src, MediaType type) {
        Media media = new Media();
        media.setId(id);
        media.setSrc(src);
        media.setType(type);
        return media;
    }
}
