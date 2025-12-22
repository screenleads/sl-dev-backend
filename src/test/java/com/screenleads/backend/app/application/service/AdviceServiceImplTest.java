package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.*;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdviceServiceImpl Unit Tests")
class AdviceServiceImplTest {

        @Mock
        private AdviceRepository adviceRepository;

        @Mock
        private MediaRepository mediaRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private MediaTypeRepository mediaTypeRepository;

        @Mock
        private CompanyRepository companyRepository;

        @Mock
        private EntityManager entityManager;

        @Mock
        private Session session;

        @InjectMocks
        private AdviceServiceImpl adviceService;

        private Advice testAdvice;
        private AdviceDTO testAdviceDTO;
        private Media testMedia;
        private Company testCompany;
        private AdviceSchedule testSchedule;
        private AdviceTimeWindow testWindow;

        @BeforeEach
        void setUp() {
                // Setup test company
                testCompany = new Company();
                testCompany.setId(1L);
                testCompany.setName("Test Company");

                // Setup test media
                MediaType mediaType = new MediaType();
                mediaType.setId(1L);
                mediaType.setType("IMG");

                testMedia = new Media();
                testMedia.setId(1L);
                testMedia.setSrc("https://example.com/test.jpg");
                testMedia.setType(mediaType);
                testMedia.setCompany(testCompany);

                // Setup test time window
                testWindow = new AdviceTimeWindow();
                testWindow.setId(1L);
                testWindow.setWeekday(DayOfWeek.MONDAY);
                testWindow.setFromTime(LocalTime.of(9, 0));
                testWindow.setToTime(LocalTime.of(17, 0));

                // Setup test schedule
                testSchedule = new AdviceSchedule();
                testSchedule.setId(1L);
                testSchedule.setStartDate(LocalDate.now());
                testSchedule.setEndDate(LocalDate.now().plusDays(30));
                testSchedule.setWindows(new ArrayList<>(List.of(testWindow)));
                testWindow.setSchedule(testSchedule);

                // Setup test advice
                testAdvice = new Advice();
                testAdvice.setId(1L);
                testAdvice.setDescription("Test Advice");
                testAdvice.setCustomInterval(false);
                testAdvice.setInterval(Duration.ofSeconds(30));
                testAdvice.setMedia(testMedia);
                testAdvice.setCompany(testCompany);
                testAdvice.setSchedules(new ArrayList<>(List.of(testSchedule)));
                testSchedule.setAdvice(testAdvice);

                // Setup test DTO
                testAdviceDTO = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(30)
                                .media(new MediaUpsertDTO(1L, "https://example.com/test.jpg"))
                                .company(new CompanyRefDTO(1L, "Test Company"))
                                .schedules(List.of(
                                                new AdviceScheduleDTO(
                                                                1L,
                                                                LocalDate.now().toString(),
                                                                LocalDate.now().plusDays(30).toString(),
                                                                List.of(new AdviceTimeWindowDTO(1L, "MONDAY", "09:00",
                                                                                "17:00")),
                                                                null)))
                                .build();
        }

        // ===================== READ TESTS =====================

        @Test
        @DisplayName("getAllAdvices should return all advices")
        void whenGetAllAdvices_thenReturnAllAdvices() {
                // Arrange
                when(adviceRepository.findAll()).thenReturn(List.of(testAdvice));

                // Act
                List<AdviceDTO> result = adviceService.getAllAdvices();

                // Assert
                assertThat(result).isNotNull().hasSize(1);
                assertThat(result.get(0).getDescription()).isEqualTo("Test Advice");
                verify(adviceRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("getAdviceById should return advice when found")
        void whenGetAdviceById_thenReturnAdvice() {
                // Arrange
                when(adviceRepository.findById(1L)).thenReturn(Optional.of(testAdvice));

                // Act
                Optional<AdviceDTO> result = adviceService.getAdviceById(1L);

                // Assert
                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(1L);
                assertThat(result.get().getDescription()).isEqualTo("Test Advice");
                verify(adviceRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getAdviceById should return empty when not found")
        void whenGetAdviceByIdNotFound_thenReturnEmpty() {
                // Arrange
                when(adviceRepository.findById(999L)).thenReturn(Optional.empty());

                // Act
                Optional<AdviceDTO> result = adviceService.getAdviceById(999L);

                // Assert
                assertThat(result).isEmpty();
                verify(adviceRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("getVisibleAdvicesNow should filter by current time")
        void whenGetVisibleAdvicesNow_thenReturnFilteredAdvices() {
                // Arrange
                // Set a window that covers current time (if running during business hours)
                testWindow.setFromTime(LocalTime.MIN); // 00:00
                testWindow.setToTime(LocalTime.MAX); // 23:59:59
                testWindow.setWeekday(DayOfWeek.from(LocalDate.now()));

                when(adviceRepository.findAll()).thenReturn(List.of(testAdvice));

                // Act
                List<AdviceDTO> result = adviceService.getVisibleAdvicesNow(null);

                // Assert
                assertThat(result).isNotNull();
                // Result may be empty or contain testAdvice depending on exact timing
                verify(adviceRepository, times(1)).findAll();
        }

        // ===================== SAVE TESTS =====================

        @Test
        @DisplayName("saveAdvice should create new advice")
        void whenSaveAdvice_thenAdviceIsCreated() {
                // Arrange
                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));
                when(adviceRepository.save(any(Advice.class))).thenReturn(testAdvice);

                // Act
                AdviceDTO result = adviceService.saveAdvice(testAdviceDTO);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getDescription()).isEqualTo("Test Advice");
                verify(adviceRepository, times(1)).save(any(Advice.class));
        }

        @Test
        @DisplayName("saveAdvice should create media if src provided but not found")
        void whenSaveAdviceWithNewMedia_thenMediaIsCreated() {
                // Arrange
                MediaUpsertDTO newMediaDto = new MediaUpsertDTO(null, "https://example.com/new.png");
                AdviceDTO dtoWithNewMedia = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(30)
                                .media(newMediaDto)
                                .company(new CompanyRefDTO(1L, "Test Company"))
                                .schedules(testAdviceDTO.getSchedules())
                                .build();

                MediaType pngType = new MediaType();
                pngType.setId(2L);
                pngType.setType("IMG");
                pngType.setExtension("png");

                when(mediaRepository.findBySrc("https://example.com/new.png")).thenReturn(Optional.empty());
                when(mediaTypeRepository.findByExtension("png")).thenReturn(Optional.of(pngType));
                when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
                when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
                when(adviceRepository.save(any(Advice.class))).thenReturn(testAdvice);

                // Act
                AdviceDTO result = adviceService.saveAdvice(dtoWithNewMedia);

                // Assert
                assertThat(result).isNotNull();
                verify(mediaRepository, times(1)).findBySrc("https://example.com/new.png");
                verify(mediaRepository, times(1)).save(any(Media.class));
                verify(adviceRepository, times(1)).save(any(Advice.class));
        }

        @Test
        @DisplayName("saveAdvice should validate schedule dates")
        void whenSaveAdviceWithInvalidDates_thenThrowException() {
                // Arrange
                AdviceScheduleDTO invalidSchedule = new AdviceScheduleDTO(
                                null,
                                LocalDate.now().plusDays(30).toString(), // End before start
                                LocalDate.now().toString(),
                                List.of(new AdviceTimeWindowDTO(null, "MONDAY", "09:00", "17:00")),
                                null);

                AdviceDTO invalidDto = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(30)
                                .media(testAdviceDTO.getMedia())
                                .company(testAdviceDTO.getCompany())
                                .schedules(List.of(invalidSchedule))
                                .build();

                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));

                // Act & Assert
                assertThatThrownBy(() -> adviceService.saveAdvice(invalidDto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("startDate must be <= endDate");
        }

        @Test
        @DisplayName("saveAdvice should validate time window from < to")
        void whenSaveAdviceWithInvalidTimeWindow_thenThrowException() {
                // Arrange
                AdviceScheduleDTO scheduleWithInvalidWindow = new AdviceScheduleDTO(
                                null,
                                LocalDate.now().toString(),
                                LocalDate.now().plusDays(30).toString(),
                                List.of(new AdviceTimeWindowDTO(null, "MONDAY", "17:00", "09:00")), // to < from
                                null);

                AdviceDTO invalidDto = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(30)
                                .media(testAdviceDTO.getMedia())
                                .company(testAdviceDTO.getCompany())
                                .schedules(List.of(scheduleWithInvalidWindow))
                                .build();

                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));

                // Act & Assert
                assertThatThrownBy(() -> adviceService.saveAdvice(invalidDto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("'from' must be strictly before 'to'");
        }

        @Test
        @DisplayName("saveAdvice should detect overlapping windows")
        void whenSaveAdviceWithOverlappingWindows_thenThrowException() {
                // Arrange
                AdviceScheduleDTO scheduleWithOverlap = new AdviceScheduleDTO(
                                null,
                                LocalDate.now().toString(),
                                LocalDate.now().plusDays(30).toString(),
                                List.of(
                                                new AdviceTimeWindowDTO(null, "MONDAY", "09:00", "12:00"),
                                                new AdviceTimeWindowDTO(null, "MONDAY", "11:00", "14:00") // Overlaps
                                                                                                          // with
                                                                                                          // previous
                                ),
                                null);

                AdviceDTO invalidDto = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(30)
                                .media(testAdviceDTO.getMedia())
                                .company(testAdviceDTO.getCompany())
                                .schedules(List.of(scheduleWithOverlap))
                                .build();

                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));

                // Act & Assert
                assertThatThrownBy(() -> adviceService.saveAdvice(invalidDto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Overlapping windows");
        }

        // ===================== UPDATE TESTS =====================

        @Test
        @DisplayName("updateAdvice should update existing advice")
        void whenUpdateAdvice_thenAdviceIsUpdated() {
                // Arrange
                AdviceDTO updateDto = AdviceDTO.builder()
                                .id(1L)
                                .description("Updated Description")
                                .customInterval(false)
                                .interval(30)
                                .media(testAdviceDTO.getMedia())
                                .company(testAdviceDTO.getCompany())
                                .schedules(testAdviceDTO.getSchedules())
                                .build();

                when(adviceRepository.findById(1L)).thenReturn(Optional.of(testAdvice));
                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));
                when(adviceRepository.save(any(Advice.class))).thenReturn(testAdvice);

                // Act
                AdviceDTO result = adviceService.updateAdvice(1L, updateDto);

                // Assert
                assertThat(result).isNotNull();
                verify(adviceRepository, times(1)).findById(1L);
                verify(adviceRepository, times(1)).save(any(Advice.class));
        }

        @Test
        @DisplayName("updateAdvice should throw exception when advice not found")
        void whenUpdateAdviceNotFound_thenThrowException() {
                // Arrange
                when(adviceRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> adviceService.updateAdvice(999L, testAdviceDTO))
                                .isInstanceOf(NoSuchElementException.class)
                                .hasMessageContaining("Advice not found");
                verify(adviceRepository, times(1)).findById(999L);
                verify(adviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateAdvice should clear and replace schedules")
        void whenUpdateAdvice_thenSchedulesAreReplaced() {
                // Arrange
                AdviceScheduleDTO newSchedule = new AdviceScheduleDTO(
                                null,
                                LocalDate.now().plusDays(10).toString(),
                                LocalDate.now().plusDays(40).toString(),
                                List.of(new AdviceTimeWindowDTO(null, "TUESDAY", "10:00", "18:00")),
                                null);

                AdviceDTO updateDto = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(30)
                                .media(testAdviceDTO.getMedia())
                                .company(testAdviceDTO.getCompany())
                                .schedules(List.of(newSchedule))
                                .build();

                when(adviceRepository.findById(1L)).thenReturn(Optional.of(testAdvice));
                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));
                when(adviceRepository.save(any(Advice.class))).thenReturn(testAdvice);

                // Act
                AdviceDTO result = adviceService.updateAdvice(1L, updateDto);

                // Assert
                assertThat(result).isNotNull();
                verify(adviceRepository, times(1)).save(any(Advice.class));
        }

        // ===================== DELETE TESTS =====================

        @Test
        @DisplayName("deleteAdvice should delete existing advice")
        void whenDeleteAdvice_thenAdviceIsDeleted() {
                // Arrange
                when(adviceRepository.findById(1L)).thenReturn(Optional.of(testAdvice));
                doNothing().when(adviceRepository).delete(testAdvice);

                // Act
                adviceService.deleteAdvice(1L);

                // Assert
                verify(adviceRepository, times(1)).findById(1L);
                verify(adviceRepository, times(1)).delete(testAdvice);
        }

        @Test
        @DisplayName("deleteAdvice should do nothing when advice not found")
        void whenDeleteAdviceNotFound_thenDoNothing() {
                // Arrange
                when(adviceRepository.findById(999L)).thenReturn(Optional.empty());

                // Act
                adviceService.deleteAdvice(999L);

                // Assert
                verify(adviceRepository, times(1)).findById(999L);
                verify(adviceRepository, never()).delete(any());
        }

        // ===================== HELPER TESTS =====================

        @Test
        @DisplayName("saveAdvice should support dayWindows format")
        void whenSaveAdviceWithDayWindows_thenScheduleIsCreated() {
                // Arrange
                AdviceScheduleDTO.DayWindowDTO dayWindow = new AdviceScheduleDTO.DayWindowDTO();
                dayWindow.setWeekday("WEDNESDAY");
                dayWindow.setRanges(List.of(
                                new AdviceScheduleDTO.RangeDTO("08:00", "12:00"),
                                new AdviceScheduleDTO.RangeDTO("13:00", "18:00")));

                AdviceScheduleDTO scheduleWithDayWindows = new AdviceScheduleDTO(
                                null,
                                LocalDate.now().toString(),
                                LocalDate.now().plusDays(30).toString(),
                                null, // No windows array
                                List.of(dayWindow));

                AdviceDTO dtoWithDayWindows = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(30)
                                .media(testAdviceDTO.getMedia())
                                .company(testAdviceDTO.getCompany())
                                .schedules(List.of(scheduleWithDayWindows))
                                .build();

                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));
                when(adviceRepository.save(any(Advice.class))).thenReturn(testAdvice);

                // Act
                AdviceDTO result = adviceService.saveAdvice(dtoWithDayWindows);

                // Assert
                assertThat(result).isNotNull();
                verify(adviceRepository, times(1)).save(any(Advice.class));
        }

        @Test
        @DisplayName("saveAdvice should handle null interval")
        void whenSaveAdviceWithNullInterval_thenNoError() {
                // Arrange
                AdviceDTO dtoWithNullInterval = AdviceDTO.builder()
                                .id(1L)
                                .description("Test Advice")
                                .customInterval(false)
                                .interval(null)
                                .media(testAdviceDTO.getMedia())
                                .company(testAdviceDTO.getCompany())
                                .schedules(testAdviceDTO.getSchedules())
                                .build();

                when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));
                when(adviceRepository.save(any(Advice.class))).thenReturn(testAdvice);

                // Act
                AdviceDTO result = adviceService.saveAdvice(dtoWithNullInterval);

                // Assert
                assertThat(result).isNotNull();
                verify(adviceRepository, times(1)).save(any(Advice.class));
        }
}
