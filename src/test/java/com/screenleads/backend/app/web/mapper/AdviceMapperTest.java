package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.web.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdviceMapper Unit Tests")
class AdviceMapperTest {

    @Test
    @DisplayName("toDto should convert Advice entity to AdviceDTO with all fields")
    void whenToDto_thenConvertAllFields() {
        // Arrange
        Media media = new Media();
        media.setId(10L);
        media.setSrc("video.mp4");

        Promotion promotion = new Promotion();
        promotion.setId(20L);

        Company company = new Company();
        company.setId(30L);
        company.setName("Test Company");

        Advice advice = new Advice();
        advice.setId(1L);
        advice.setDescription("Test Advice");
        advice.setCustomInterval(true);
        advice.setInterval(Duration.ofSeconds(120));
        advice.setMedia(media);
        advice.setPromotion(promotion);
        advice.setCompany(company);

        AdviceTimeWindow window1 = new AdviceTimeWindow();
        window1.setId(100L);
        window1.setWeekday(DayOfWeek.MONDAY);
        window1.setFromTime(LocalTime.of(9, 0));
        window1.setToTime(LocalTime.of(17, 0));

        AdviceSchedule schedule = new AdviceSchedule();
        schedule.setId(50L);
        schedule.setStartDate(LocalDate.of(2025, 1, 1));
        schedule.setEndDate(LocalDate.of(2025, 12, 31));
        schedule.setWindows(List.of(window1));

        advice.setSchedules(List.of(schedule));

        // Act
        AdviceDTO result = AdviceMapper.toDto(advice);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Test Advice");
        assertThat(result.getCustomInterval()).isTrue();
        assertThat(result.getInterval()).isEqualTo(120L);
        
        assertThat(result.getMedia()).isNotNull();
        assertThat(result.getMedia().id()).isEqualTo(10L);
        assertThat(result.getMedia().src()).isEqualTo("video.mp4");
        
        assertThat(result.getPromotion()).isNotNull();
        assertThat(result.getPromotion().id()).isEqualTo(20L);
        
        assertThat(result.getCompany()).isNotNull();
        assertThat(result.getCompany().id()).isEqualTo(30L);
        assertThat(result.getCompany().name()).isEqualTo("Test Company");
        
        assertThat(result.getSchedules()).hasSize(1);
        AdviceScheduleDTO scheduleDto = result.getSchedules().get(0);
        assertThat(scheduleDto.getId()).isEqualTo(50L);
        assertThat(scheduleDto.getStartDate()).isEqualTo("2025-01-01");
        assertThat(scheduleDto.getEndDate()).isEqualTo("2025-12-31");
        
        assertThat(scheduleDto.getWindows()).hasSize(1);
        AdviceTimeWindowDTO windowDto = scheduleDto.getWindows().get(0);
        assertThat(windowDto.getId()).isEqualTo(100L);
        assertThat(windowDto.getWeekday()).isEqualTo("MONDAY");
        assertThat(windowDto.getFromTime()).isEqualTo("09:00");
        assertThat(windowDto.getToTime()).isEqualTo("17:00");
    }

    @Test
    @DisplayName("toDto should handle null interval")
    void whenToDtoWithNullInterval_thenIntervalIsNull() {
        // Arrange
        Advice advice = new Advice();
        advice.setId(1L);
        advice.setDescription("Test");
        advice.setInterval(null);

        // Act
        AdviceDTO result = AdviceMapper.toDto(advice);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInterval()).isNull();
    }

    @Test
    @DisplayName("toDto should return null when Advice is null")
    void whenToDtoWithNull_thenReturnNull() {
        // Act
        AdviceDTO result = AdviceMapper.toDto(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDTO alias should work same as toDto")
    void whenToDTOAlias_thenWorksSameAsToDto() {
        // Arrange
        Advice advice = new Advice();
        advice.setId(1L);
        advice.setDescription("Test");

        // Act
        AdviceDTO result = AdviceMapper.toDTO(advice);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("toDto should handle multiple schedules with multiple windows")
    void whenToDtoWithMultipleSchedules_thenConvertAll() {
        // Arrange
        AdviceTimeWindow window1 = new AdviceTimeWindow();
        window1.setId(1L);
        window1.setWeekday(DayOfWeek.MONDAY);
        window1.setFromTime(LocalTime.of(9, 0));
        window1.setToTime(LocalTime.of(12, 0));

        AdviceTimeWindow window2 = new AdviceTimeWindow();
        window2.setId(2L);
        window2.setWeekday(DayOfWeek.WEDNESDAY);
        window2.setFromTime(LocalTime.of(14, 0));
        window2.setToTime(LocalTime.of(18, 0));

        AdviceSchedule schedule1 = new AdviceSchedule();
        schedule1.setId(10L);
        schedule1.setStartDate(LocalDate.of(2025, 1, 1));
        schedule1.setEndDate(LocalDate.of(2025, 6, 30));
        schedule1.setWindows(Arrays.asList(window1, window2));

        AdviceSchedule schedule2 = new AdviceSchedule();
        schedule2.setId(20L);
        schedule2.setStartDate(LocalDate.of(2025, 7, 1));
        schedule2.setEndDate(LocalDate.of(2025, 12, 31));
        schedule2.setWindows(List.of(window1));

        Advice advice = new Advice();
        advice.setId(1L);
        advice.setDescription("Multi-schedule advice");
        advice.setSchedules(Arrays.asList(schedule1, schedule2));

        // Act
        AdviceDTO result = AdviceMapper.toDto(advice);

        // Assert
        assertThat(result.getSchedules()).hasSize(2);
        assertThat(result.getSchedules().get(0).getWindows()).hasSize(2);
        assertThat(result.getSchedules().get(1).getWindows()).hasSize(1);
    }

    @Test
    @DisplayName("toEntity should convert AdviceDTO to Advice entity")
    void whenToEntity_thenConvertAllFields() {
        // Arrange
        MediaUpsertDTO mediaDto = new MediaUpsertDTO(10L, "video.mp4");
        PromotionRefDTO promoDto = new PromotionRefDTO(20L);
        CompanyRefDTO companyDto = new CompanyRefDTO(30L, "Test Company");

        AdviceTimeWindowDTO windowDto = new AdviceTimeWindowDTO(
                null, "TUESDAY", "10:00", "16:00");

        AdviceScheduleDTO scheduleDto = new AdviceScheduleDTO(
                null, "2025-01-15", "2025-06-15", List.of(windowDto), null);

        AdviceDTO dto = AdviceDTO.builder()
                .id(1L)
                .description("Test Advice")
                .customInterval(true)
                .interval(180)
                .media(mediaDto)
                .promotion(promoDto)
                .company(companyDto)
                .schedules(List.of(scheduleDto))
                .build();

        // Act
        Advice result = AdviceMapper.toEntity(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Test Advice");
        assertThat(result.getCustomInterval()).isTrue();
        assertThat(result.getInterval()).isEqualTo(Duration.ofSeconds(180));
        
        assertThat(result.getMedia()).isNotNull();
        assertThat(result.getMedia().getId()).isEqualTo(10L);
        
        assertThat(result.getPromotion()).isNotNull();
        assertThat(result.getPromotion().getId()).isEqualTo(20L);
        
        assertThat(result.getCompany()).isNotNull();
        assertThat(result.getCompany().getId()).isEqualTo(30L);
        
        assertThat(result.getSchedules()).hasSize(1);
        AdviceSchedule schedule = result.getSchedules().get(0);
        assertThat(schedule.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(schedule.getEndDate()).isEqualTo(LocalDate.of(2025, 6, 15));
        assertThat(schedule.getAdvice()).isEqualTo(result);
        
        assertThat(schedule.getWindows()).hasSize(1);
        AdviceTimeWindow window = schedule.getWindows().get(0);
        assertThat(window.getWeekday()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(window.getFromTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(window.getToTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(window.getSchedule()).isEqualTo(schedule);
    }

    @Test
    @DisplayName("toEntity should return null when AdviceDTO is null")
    void whenToEntityWithNull_thenReturnNull() {
        // Act
        Advice result = AdviceMapper.toEntity(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toEntity should handle null interval")
    void whenToEntityWithNullInterval_thenIntervalIsNull() {
        // Arrange
        AdviceDTO dto = AdviceDTO.builder()
                .id(1L)
                .description("Test")
                .interval(null)
                .build();

        // Act
        Advice result = AdviceMapper.toEntity(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInterval()).isNull();
    }

    @Test
    @DisplayName("toEntity should handle null references")
    void whenToEntityWithNullRefs_thenRefsAreNull() {
        // Arrange
        AdviceDTO dto = AdviceDTO.builder()
                .id(1L)
                .description("Test")
                .media(null)
                .promotion(null)
                .company(null)
                .build();

        // Act
        Advice result = AdviceMapper.toEntity(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMedia()).isNull();
        assertThat(result.getPromotion()).isNull();
        assertThat(result.getCompany()).isNull();
    }

    @Test
    @DisplayName("toEntity should handle null schedules")
    void whenToEntityWithNullSchedules_thenSchedulesIsEmpty() {
        // Arrange
        AdviceDTO dto = AdviceDTO.builder()
                .id(1L)
                .description("Test")
                .schedules(null)
                .build();

        // Act
        Advice result = AdviceMapper.toEntity(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSchedules()).isEmpty();
    }

    @Test
    @DisplayName("toEntity should handle schedule with null windows")
    void whenToEntityWithScheduleNullWindows_thenWindowsIsEmpty() {
        // Arrange
        AdviceScheduleDTO scheduleDto = new AdviceScheduleDTO(
                null, "2025-01-01", "2025-12-31", null, null);

        AdviceDTO dto = AdviceDTO.builder()
                .id(1L)
                .description("Test")
                .schedules(List.of(scheduleDto))
                .build();

        // Act
        Advice result = AdviceMapper.toEntity(dto);

        // Assert
        assertThat(result.getSchedules()).hasSize(1);
        assertThat(result.getSchedules().get(0).getWindows()).isEmpty();
    }
}
