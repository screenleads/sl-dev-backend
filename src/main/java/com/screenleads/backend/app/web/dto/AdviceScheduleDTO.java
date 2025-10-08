package com.screenleads.backend.app.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public class AdviceScheduleDTO {
    private Long id;
    private String startDate; // "YYYY-MM-DD" o null
    private String endDate;   // "YYYY-MM-DD" o null
    private List<AdviceTimeWindowDTO> windows;

    // Para compatibilidad con payloads agrupados por día
    @JsonProperty("dayWindows")
    private List<DayWindowDTO> dayWindows;

    public AdviceScheduleDTO() {}

    public AdviceScheduleDTO(Long id, String startDate, String endDate, List<AdviceTimeWindowDTO> windows, List<DayWindowDTO> dayWindows) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.windows = windows;
        this.dayWindows = dayWindows;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<AdviceTimeWindowDTO> getWindows() { return windows; }
    public void setWindows(List<AdviceTimeWindowDTO> windows) { this.windows = windows; }
    public List<DayWindowDTO> getDayWindows() { return dayWindows; }
    public void setDayWindows(List<DayWindowDTO> dayWindows) { this.dayWindows = dayWindows; }

    public static class DayWindowDTO {
        private String weekday;
        private List<RangeDTO> ranges;

        public DayWindowDTO() {}
        public DayWindowDTO(String weekday, List<RangeDTO> ranges) {
            this.weekday = weekday;
            this.ranges = ranges;
        }
        public String getWeekday() { return weekday; }
        public void setWeekday(String weekday) { this.weekday = weekday; }
        public List<RangeDTO> getRanges() { return ranges; }
        public void setRanges(List<RangeDTO> ranges) { this.ranges = ranges; }
    }

    public static class RangeDTO {
        private String fromTime;
        private String toTime;

        public RangeDTO() {}
        public RangeDTO(String fromTime, String toTime) {
            this.fromTime = fromTime;
            this.toTime = toTime;
        }
        public String getFromTime() { return fromTime; }
        public void setFromTime(String fromTime) { this.fromTime = fromTime; }
        public String getToTime() { return toTime; }
        public void setToTime(String toTime) { this.toTime = toTime; }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdviceScheduleDTO that = (AdviceScheduleDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                Objects.equals(windows, that.windows) &&
                Objects.equals(dayWindows, that.dayWindows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startDate, endDate, windows, dayWindows);
    }
}
