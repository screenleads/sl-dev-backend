package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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

import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.MediaDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaServiceImpl Unit Tests")
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaTypeRepository mediaTypeRepository;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Media testMedia;
    private MediaType testMediaType;

    @BeforeEach
    void setUp() {
        // Setup test MediaType
        testMediaType = new MediaType();
        testMediaType.setId(1L);
        testMediaType.setType("IMAGE");
        testMediaType.setExtension("jpg");
        testMediaType.setEnabled(true);

        // Setup test Media entity
        testMedia = new Media();
        testMedia.setId(1L);
        testMedia.setSrc("https://example.com/image.jpg");
        testMedia.setType(testMediaType);

        // Setup test MediaDTO
    }

    @Test
    @DisplayName("getAllMedias should return all medias when repository has data")
    void whenGetAllMedias_thenReturnsAllMedias() {
        // Arrange
        Media media2 = new Media();
        media2.setId(2L);
        media2.setSrc("https://example.com/video.mp4");
        media2.setType(testMediaType);

        when(mediaRepository.findAll()).thenReturn(Arrays.asList(testMedia, media2));

        // Act
        List<MediaDTO> result = mediaService.getAllMedias();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(0).src()).isEqualTo("https://example.com/image.jpg");
        verify(mediaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllMedias should return empty list when repository is empty")
    void whenGetAllMediasOnEmptyRepo_thenReturnsEmptyList() {
        // Arrange
        when(mediaRepository.findAll()).thenReturn(List.of());

        // Act
        List<MediaDTO> result = mediaService.getAllMedias();

        // Assert
        assertThat(result).isEmpty();
        verify(mediaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllMedias should return sorted list by id")
    void whenGetAllMedias_thenReturnsSortedById() {
        // Arrange
        Media media2 = new Media();
        media2.setId(3L);
        media2.setSrc("https://example.com/video.mp4");
        media2.setType(testMediaType);

        Media media3 = new Media();
        media3.setId(2L);
        media3.setSrc("https://example.com/audio.mp3");
        media3.setType(testMediaType);

        when(mediaRepository.findAll()).thenReturn(Arrays.asList(media2, testMedia, media3));

        // Act
        List<MediaDTO> result = mediaService.getAllMedias();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(2).id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getMediaById should return media when found")
    void whenGetMediaByIdExists_thenReturnsMedia() {
        // Arrange
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));

        // Act
        Optional<MediaDTO> result = mediaService.getMediaById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().src()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.get().type()).isEqualTo(testMediaType);
        verify(mediaRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getMediaById should return empty when not found")
    void whenGetMediaByIdNotExists_thenReturnsEmpty() {
        // Arrange
        when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<MediaDTO> result = mediaService.getMediaById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(mediaRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("saveMedia should save and return media successfully")
    void whenSaveMedia_thenSavesSuccessfully() {
        // Arrange
        MediaDTO inputDTO = new MediaDTO(null, "https://example.com/new-image.jpg", testMediaType);

        Media savedMedia = new Media();
        savedMedia.setId(10L);
        savedMedia.setSrc("https://example.com/new-image.jpg");
        savedMedia.setType(testMediaType);

        when(mediaTypeRepository.findById(1L)).thenReturn(Optional.of(testMediaType));
        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Act
        MediaDTO result = mediaService.saveMedia(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.src()).isEqualTo("https://example.com/new-image.jpg");
        assertThat(result.type()).isEqualTo(testMediaType);
        verify(mediaTypeRepository, times(1)).findById(1L);
        verify(mediaRepository, times(1)).save(any(Media.class));
    }

    @Test
    @DisplayName("saveMedia should throw exception when MediaType is null")
    void whenSaveMediaWithNullType_thenThrowsException() {
        // Arrange
        MediaDTO inputDTO = new MediaDTO(null, "https://example.com/image.jpg", null);

        // Act & Assert
        assertThatThrownBy(() -> mediaService.saveMedia(inputDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Media type requerido");

        verify(mediaRepository, never()).save(any(Media.class));
        verify(mediaTypeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("saveMedia should throw exception when MediaType ID is null")
    void whenSaveMediaWithNullTypeId_thenThrowsException() {
        // Arrange
        MediaType typeWithoutId = new MediaType();
        typeWithoutId.setType("IMAGE");
        MediaDTO inputDTO = new MediaDTO(null, "https://example.com/image.jpg", typeWithoutId);

        // Act & Assert
        assertThatThrownBy(() -> mediaService.saveMedia(inputDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Media type requerido");

        verify(mediaRepository, never()).save(any(Media.class));
        verify(mediaTypeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("saveMedia should throw exception when MediaType not found in database")
    void whenSaveMediaWithNonExistentType_thenThrowsException() {
        // Arrange
        MediaDTO inputDTO = new MediaDTO(null, "https://example.com/image.jpg", testMediaType);
        when(mediaTypeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> mediaService.saveMedia(inputDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Media type no encontrado");

        verify(mediaTypeRepository, times(1)).findById(1L);
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    @DisplayName("updateMedia should update and return media successfully")
    void whenUpdateMediaExists_thenUpdatesSuccessfully() {
        // Arrange
        MediaDTO updateDTO = new MediaDTO(1L, "https://example.com/updated-image.jpg", testMediaType);

        Media updatedMedia = new Media();
        updatedMedia.setId(1L);
        updatedMedia.setSrc("https://example.com/updated-image.jpg");
        updatedMedia.setType(testMediaType);

        when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));
        when(mediaRepository.save(any(Media.class))).thenReturn(updatedMedia);

        // Act
        MediaDTO result = mediaService.updateMedia(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.src()).isEqualTo("https://example.com/updated-image.jpg");
        verify(mediaRepository, times(1)).findById(1L);
        verify(mediaRepository, times(1)).save(any(Media.class));
    }

    @Test
    @DisplayName("updateMedia should throw exception when media not found")
    void whenUpdateMediaNotExists_thenThrowsException() {
        // Arrange
        MediaDTO updateDTO = new MediaDTO(999L, "https://example.com/image.jpg", testMediaType);
        when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> mediaService.updateMedia(999L, updateDTO))
                .isInstanceOf(NoSuchElementException.class);

        verify(mediaRepository, times(1)).findById(999L);
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    @DisplayName("deleteMedia should delete media by id")
    void whenDeleteMedia_thenDeletesSuccessfully() {
        // Act
        mediaService.deleteMedia(1L);

        // Assert
        verify(mediaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteMedia should call repository even if media doesn't exist")
    void whenDeleteNonExistentMedia_thenCallsRepository() {
        // Act
        mediaService.deleteMedia(999L);

        // Assert
        verify(mediaRepository, times(1)).deleteById(999L);
    }
}
